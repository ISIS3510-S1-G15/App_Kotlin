package com.uniandes.campuseats

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.uniandes.campuseats.analytics.AnalyticsTransport
import com.uniandes.campuseats.analytics.HttpAnalyticsTransport
import com.uniandes.campuseats.auth.AuthRepository
import com.uniandes.campuseats.auth.SessionStore
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheStats
import com.uniandes.campuseats.data.cache.CachedDocument
import com.uniandes.campuseats.data.cache.MemoryCache
import com.uniandes.campuseats.data.cache.TieredDocumentCache
import com.uniandes.campuseats.data.local.AppDatabase
import com.uniandes.campuseats.data.local.LegacyPreferencesMigration
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.local.RoomDocumentCache
import com.uniandes.campuseats.data.local.ReviewDraftStore
import com.uniandes.campuseats.data.outbox.OutboxFlusher
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.data.outbox.OutboxStore
import com.uniandes.campuseats.data.prefs.UserPreferencesRepository
import com.uniandes.campuseats.data.ranking.RankingCache
import com.uniandes.campuseats.data.ranking.RankingRepository
import com.uniandes.campuseats.data.remote.CatalogRepository
import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.PartnerReportRepository
import com.uniandes.campuseats.data.remote.ProfileRepository
import com.uniandes.campuseats.data.remote.ProfileSynchronizer
import com.uniandes.campuseats.data.remote.RecentViewsRepository
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.SavedRepository
import com.uniandes.campuseats.net.ConnectivityMonitor
import com.uniandes.campuseats.net.ConnectivityObserver
import com.uniandes.campuseats.net.HttpRemoteApi
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
import com.uniandes.campuseats.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppServices private constructor(val appContext: Context) {
    /** Scope de proceso para trabajo que no debe morir con una pantalla (preferencias, limpieza, etc.). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- preferencias del usuario (DataStore) ----
    val preferences = UserPreferencesRepository.create(appContext, appScope)

    // ---- almacenamiento ----
    val sessionStore = SessionStore(appContext)
    val localStore = LocalStore(AppDatabase.create(appContext))
    val reviewDraftStore = ReviewDraftStore(appContext)

    // ---- cache: L1 memoria (objetos y documentos) delante de L2 disco (Room) ----
    private val initialEntries = preferences.current.value.memoryCacheEntries
    val objectMemoryCache = MemoryCache<String, Any>(initialEntries)
    val documentMemoryCache = MemoryCache<String, CachedDocument>(initialEntries * DOC_ENTRIES_FACTOR)
    val documentCache = TieredDocumentCache(RoomDocumentCache(localStore), documentMemoryCache) {
        preferences.current.value.persistCacheOnDisk
    }
    private val cachePolicy = CachePolicy(ttlScale = { preferences.current.value.cacheFreshness.multiplier })

    private val migrationLock = Mutex()
    val api: RemoteApi = HttpRemoteApi(BuildConfig.API_BASE_URL) { sessionStore.token }
    val authRepository = AuthRepository(api, sessionStore)
    val connectivity: ConnectivityMonitor = ConnectivityObserver(appContext)
    val outboxStore: OutboxStore = localStore
    val outboxFlusher = OutboxFlusher(api, outboxStore)
    val profileRepository = ProfileRepository(api)
    val profileSynchronizer = ProfileSynchronizer(localStore, profileRepository)
    val savedRepository = SavedRepository(api)
    val recentViewsRepository = RecentViewsRepository(api, localStore, cachePolicy)
    val restaurantRepository = RestaurantRepository(api, documentCache, policy = cachePolicy, memory = objectMemoryCache)
    val reviewRepository = ReviewRepository(api, documentCache, cachePolicy, objectMemoryCache)
    val catalogRepository = CatalogRepository(api, documentCache, cachePolicy, objectMemoryCache)
    val occupancyRepository = OccupancyRepository(api, documentCache, cachePolicy)
    val partnerReportRepository = PartnerReportRepository(api, documentCache, cachePolicy)
    val rankingCache = RankingCache()
    val rankingRepository = RankingRepository(restaurantRepository, reviewRepository, occupancyRepository, localStore, rankingCache, cachePolicy)
    val analyticsTransport: AnalyticsTransport = HttpAnalyticsTransport(api)
    val syncScheduler = SyncScheduler(appContext) { preferences.current.value }

    /** true cuando el servidor respondio 401 al sincronizar: la UI debe pedir iniciar sesion de nuevo. */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun markSessionExpired() {
        _sessionExpired.value = true
    }

    fun acknowledgeSessionExpired() {
        _sessionExpired.value = false
    }

    init {
        // El tamaño del cache en memoria sigue a la preferencia del usuario
        preferences.flow
            .map { it.memoryCacheEntries }
            .distinctUntilChanged()
            .onEach {
                objectMemoryCache.resize(it)
                documentMemoryCache.resize(it * DOC_ENTRIES_FACTOR)
            }
            .launchIn(appScope)

        // Programacion de trabajo en segundo plano segun preferencias (wifi-only, prefetch)
        preferences.flow
            .map { Triple(it.syncOnlyOnWifi, it.backgroundPrefetch, it.dataSaver) }
            .distinctUntilChanged()
            .onEach {
                syncScheduler.applyPeriodicRefresh()
                if (localStore.count() > 0) syncScheduler.scheduleOutboxSync()
            }
            .launchIn(appScope)

        // El sistema pide memoria: soltamos cache en memoria antes de que nos maten el proceso
        appContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                when {
                    level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> clearMemoryCaches()
                    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> trimMemoryCaches(0.25f)
                    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> trimMemoryCaches(0.5f)
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = clearMemoryCaches()
        })
    }

    suspend fun prepareLocalData() = migrationLock.withLock {
        LegacyPreferencesMigration(appContext, localStore).runOnce()
    }

    /**
     * Garantiza que los datos locales (perfil, guardados, cola de salida...) pertenecen a [userId].
     * Si quedaron de otra cuenta (p. ej. cierre de sesion por token vencido) se purgan antes de cargarlos,
     * para que nunca se envien con el token de otro usuario ni se muestren a otra persona.
     */
    suspend fun ensureDataOwner(userId: Int) {
        val owner = localStore.loadDocument(LocalStore.DATA_OWNER_KEY)?.payload
        if (owner != null && owner != userId.toString()) {
            localStore.clearUserData()
            attemptOrNull { reviewDraftStore.clearAll() }
            attemptOrNull { reviewRepository.clearMyReviewsCache() }
            clearMemoryCaches()
        }
        localStore.saveDocument(LocalStore.DATA_OWNER_KEY, userId.toString(), System.currentTimeMillis())
    }

    /**
     * Limpieza completa al cerrar sesion (un unico punto para todos los caminos de logout):
     * envia lo pendiente si hay red, borra datos del usuario, filtros y cache en memoria.
     */
    suspend fun purgeUserData(flushFirst: Boolean) = withContext(NonCancellable) {
        if (flushFirst && connectivity.online.value) {
            attemptOrNull { profileSynchronizer.syncPending() }
            attemptOrNull { outboxFlusher.flush(ignoreBackoff = true) }
        }
        attemptOrNull { localStore.clearUserData() }
        attemptOrNull { preferences.clearHomeFilters() }
        // Agregado para que los borradores de las reseñas se borren tambien al cerrar sesion
        attemptOrNull { reviewDraftStore.clearAll() }
        attemptOrNull { reviewRepository.clearMyReviewsCache() }
        clearMemoryCaches()
        syncScheduler.cancelAll()
        _sessionExpired.value = false
    }

    /** Los eventos de analitica nunca se pierden al destruirse un ViewModel ni tumban la app si Room falla. */
    suspend fun keepAnalyticsForLater(path: String, body: Map<String, Any>) {
        withContext(NonCancellable) {
            attemptOrNull { outboxStore.enqueue(OutboxRequest(OutboxKind.ANALYTICS, "POST", path, body)) }
        }
    }

    // ---- cache: operaciones para la pantalla de ajustes ----

    fun clearMemoryCaches() {
        objectMemoryCache.clear()
        documentMemoryCache.clear()
        rankingCache.clear()
        recentViewsRepository.invalidate()
    }

    private fun trimMemoryCaches(fraction: Float) {
        objectMemoryCache.trimTo(fraction)
        documentMemoryCache.trimTo(fraction)
        rankingCache.trim(fraction)
    }

    /** Borra cache en memoria + documentos cacheados en disco (no toca datos del usuario ni la cola de salida). */
    suspend fun clearAllCaches() {
        clearMemoryCaches()
        attemptOrNull { localStore.clearCachedDocuments() }
    }

    suspend fun diskCacheReport(): DiskCacheReport = DiskCacheReport(
        documents = attemptOrNull { localStore.documentCount() } ?: 0,
        bytes = attemptOrNull { localStore.documentBytes() } ?: 0L
    )

    fun memoryCacheStats(): List<Pair<String, StateFlow<CacheStats>>> = listOf(
        "Parsed objects" to objectMemoryCache.stats,
        "Documents" to documentMemoryCache.stats,
        "Recent views sync" to recentViewsRepository.cacheStats
    )

    data class DiskCacheReport(val documents: Int, val bytes: Long)

    companion object {
        private const val DOC_ENTRIES_FACTOR = 2

        @Volatile
        private var instance: AppServices? = null

        fun get(context: Context): AppServices = instance ?: synchronized(this) {
            instance ?: AppServices(context.applicationContext).also { instance = it }
        }
    }
}
