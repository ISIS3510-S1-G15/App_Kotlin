package com.uniandes.campuseats

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import com.uniandes.campuseats.analytics.AnalyticsScreen
import com.uniandes.campuseats.analytics.AnalyticsViewModel
import com.uniandes.campuseats.analytics.AppSection
import com.uniandes.campuseats.analytics.SectionAction
import com.uniandes.campuseats.analytics.SectionAnalyticsViewModel
import com.uniandes.campuseats.data.MealRules
import com.uniandes.campuseats.data.remote.applyRatingSummaries
import com.uniandes.campuseats.auth.AuthUser
import com.uniandes.campuseats.auth.AuthViewModel
import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.emptyProfile
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.prefs.HomeFilterState
import com.uniandes.campuseats.data.remote.appLevelFromRemote
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.net.attemptOrNull
import com.uniandes.campuseats.sensor.distanceMeters
import com.uniandes.campuseats.sensor.rememberUserLocation
import com.uniandes.campuseats.ui.ConnectionErrorView
import com.uniandes.campuseats.ui.CrowdingModal
import com.uniandes.campuseats.ui.DetailView
import com.uniandes.campuseats.ui.HomeFilters
import com.uniandes.campuseats.ui.HomeView
import com.uniandes.campuseats.ui.LoadingView
import com.uniandes.campuseats.ui.LocalCatalog
import com.uniandes.campuseats.ui.LocalMealRules
import com.uniandes.campuseats.ui.LocalRestaurants
import com.uniandes.campuseats.ui.LoginView
import com.uniandes.campuseats.ui.MapView
import com.uniandes.campuseats.ui.OfflineBanner
import com.uniandes.campuseats.ui.PendingSyncBanner
import com.uniandes.campuseats.ui.ProfileView
import com.uniandes.campuseats.ui.ReviewsListView
import com.uniandes.campuseats.ui.SavedView
import com.uniandes.campuseats.ui.SearchView
import com.uniandes.campuseats.ui.SettingsView
import com.uniandes.campuseats.ui.SurveyView
import com.uniandes.campuseats.ui.WriteReviewView
import com.uniandes.campuseats.ui.PartnerReportView
import com.uniandes.campuseats.ui.RankingView
import com.uniandes.campuseats.ui.MyReviewsView
import com.uniandes.campuseats.ui.RecentlyViewedView
import com.uniandes.campuseats.ui.resolveRecentlyViewed
import com.uniandes.campuseats.data.remote.RecentView
import com.uniandes.campuseats.ranking.RankingViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CampusEatsMainScreen()
            }
        }
    }
}

/** Pantallas que se apilan encima de las tabs (igual que los overlays del prototipo). */
private enum class Overlay { Detail, WriteReview, ReviewsList, Survey, PartnerReport, Settings, Ranking, MyReviews, RecentlyViewed }
private const val MAP_TAB = 2
private const val FAVORITES_TAB = 3
private const val PROFILE_TAB = 4

@Composable
fun CampusEatsMainScreen() {
    val context = LocalContext.current
    val services = remember { AppServices.get(context) }
    val auth: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(
            services.authRepository,
            services.localStore,
            services.connectivity,
            services::purgeUserData
        )
    )
    val user = auth.user

    // El worker de segundo plano (o un flush) recibio 401: se pide iniciar sesion de nuevo conservando la cola
    val sessionExpired by services.sessionExpired.collectAsStateWithLifecycle()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            auth.onSessionExpired()
            services.acknowledgeSessionExpired()
        }
    }

    when {
        auth.restoring -> LoadingView()
        user == null -> LoginView(
            loading = auth.loading,
            error = auth.error,
            onLogin = auth::login,
            onRegister = auth::register,
            onInputChanged = auth::clearError
        )
        else -> key(user.id) {
            CampusEatsApp(
                services = services,
                user = user,
                auth = auth
            )
        }
    }
}

@Composable
private fun CampusEatsApp(services: AppServices, user: AuthUser, auth: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStore = services.localStore
    var savedHome by remember { mutableStateOf(HomeFilterState()) }
    val prefs by services.preferences.current.collectAsStateWithLifecycle()
    val analytics: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(services))
    val sections: SectionAnalyticsViewModel = viewModel(factory = SectionAnalyticsViewModel.factory(services))

    var profileResolved by remember { mutableStateOf(false) }
    var surveyDone by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(emptyProfile) }
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    // Pila de pantallas y restaurante elegido sobreviven a rotacion / muerte de proceso
    val overlayStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Overlay>, String>(
            save = { stack -> stack.map { it.name } },
            restore = { names -> names.mapNotNull { runCatching { Overlay.valueOf(it) }.getOrNull() }.toMutableStateList() }
        )
    ) { mutableStateListOf<Overlay>() }
    val overlay = overlayStack.lastOrNull()
    var selectedRestaurantId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    val reviews = remember { mutableStateListOf<Review>() }
    var myReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var pendingMyReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var myReviewsLoading by remember { mutableStateOf(false) }
    val crowding = remember { mutableStateMapOf<String, List<Int>>() }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }
    var restaurants by remember { mutableStateOf(emptyList<Restaurant>()) }
    val selectedRestaurant = selectedRestaurantId?.let { id -> restaurants.firstOrNull { it.id == id } }
    var restaurantsResolved by remember { mutableStateOf(false) }
    var restaurantsReachable by remember { mutableStateOf(false) }
    var catalogReachable by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    val recentViewsFlow = remember { services.recentViewsRepository.observe() }
    val recentViews by recentViewsFlow.collectAsStateWithLifecycle(initialValue = emptyList<RecentView>())
    var recentViewsUpdatedAt by remember { mutableStateOf<Long?>(null) }
    var recentViewsPending by remember { mutableIntStateOf(0) }
    var recentViewsSyncing by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(Catalog.Empty) }
    var catalogResolved by remember { mutableStateOf(false) }

    var startupDone by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var syncInFlight by remember { mutableStateOf(false) }
    var outboxPending by remember { mutableIntStateOf(0) }
    // Fuente unica de verdad de conectividad (red con internet validado)
    val online by services.connectivity.online.collectAsStateWithLifecycle()
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }
    var reviewsLastSyncTime by remember { mutableStateOf<Long?>(null) }
    var myReviewsLastSyncTime by remember { mutableStateOf<Long?>(null) }

    // Cambios hechos offline que aun no llegaron al servidor: se re-aplican encima de lo que baje de la red
    suspend fun pendingCrowding(): Map<String, List<Int>> =
        (attemptOrNull { localStore.pending() }.orEmpty())
            .filter { it.request.kind == OutboxKind.OCCUPANCY }
            .mapNotNull { entry ->
                val body = entry.request.body ?: return@mapNotNull null
                val id = body["restaurant_id"]?.toString() ?: return@mapNotNull null
                val level = (body["level"] as? Number)?.toInt() ?: return@mapNotNull null
                id to appLevelFromRemote(level)
            }
            .groupBy({ it.first }, { it.second })

    suspend fun applyPendingSaved(base: Set<String>): Set<String> {
        var result = base
        (attemptOrNull { localStore.pending() }.orEmpty())
            .filter { it.request.kind == OutboxKind.SAVED }
            .forEach { entry ->
                val id = entry.request.path.substringAfterLast('/')
                result = if (entry.request.method == "DELETE") result - id else result + id
            }
        return result
    }

    suspend fun updatePending() {
        val queued = attemptOrNull { localStore.pendingCountExcludingAnalytics() } ?: 0
        val profilePending = if (attemptOrNull { services.profileSynchronizer.hasPending() } == true) 1 else 0
        outboxPending = queued + profilePending
        recentViewsPending = attemptOrNull { localStore.pending() }.orEmpty()
            .count { services.recentViewsRepository.isRecentViewsRequest(it.request) }
    }

    LaunchedEffect(overlay, outboxPending, online) {
        if (overlay == Overlay.MyReviews) {
            // Lee las reseñas que estan guardadas en el Room
            val entries = attemptOrNull { services.localStore.pending() }.orEmpty()
                .filter { it.request.kind == OutboxKind.REVIEW }

            pendingMyReviews = entries.mapNotNull { entry ->
                val body = entry.request.body ?: return@mapNotNull null
                val restaurantId = entry.request.path
                    .removePrefix("/restaurants/")
                    .removeSuffix("/reviews")
                try {
                    val ratings = ReviewRatings(
                        taste = (body["taste"] as? Number)?.toInt() ?: 0,
                        attention = (body["attention"] as? Number)?.toInt() ?: 0,
                        price = (body["price"] as? Number)?.toInt() ?: 0,
                        options = (body["options"] as? Number)?.toInt() ?: 0
                    )
                    val dietaryOptions = (body["dietary_options"] as? List<*>)
                        ?.map { it.toString() }
                        .orEmpty()

                    Review(
                        id = "pending_${entry.id}",
                        restaurantId = restaurantId,
                        author = profile.name.ifEmpty { user.name },
                        date = "Pending sync...",
                        ratings = ratings,
                        dietaryOptions = dietaryOptions,
                        comment = (body["comment"] as? String).orEmpty()
                    )

                } catch (e: Exception) {
                    null
                }
            }

            myReviewsLastSyncTime = services.reviewRepository.myReviewsLastSync()
            myReviewsLoading = true
            myReviews = if (online) {
                attemptOrNull { services.reviewRepository.myReviews() }
                    ?: services.reviewRepository.cachedMyReviews()
            } else {
                services.reviewRepository.cachedMyReviews()
            }
            myReviewsLastSyncTime = services.reviewRepository.myReviewsLastSync()
            myReviewsLoading = false
        }
    }

    suspend fun refreshRestaurants(force: Boolean = false) {
        val result = attemptOrNull { services.restaurantRepository.revalidate(force) }
        if (result != null) restaurantsReachable = true
        val fetched = result?.takeIf { it.isNotEmpty() }
        val freshSummaries = attemptOrNull { services.reviewRepository.revalidateSummaries(force) }
        val base = fetched ?: restaurants.takeIf { freshSummaries != null && it.isNotEmpty() } ?: return
        val summaries = freshSummaries ?: services.reviewRepository.cachedSummaries()
        restaurants = applyRatingSummaries(base, summaries)
    }

    suspend fun refreshCatalog(force: Boolean = false) {
        val result = attemptOrNull { services.catalogRepository.revalidate(force) }
        if (result != null) catalogReachable = true
        result?.takeIf { !it.isEmpty }?.let { catalog = it }
    }

    suspend fun refreshReviews(force: Boolean = false) {
        val current = reviews.toList()
        val ids = restaurants.map { it.id }
        val fetched = services.reviewRepository.revalidateAll(ids, force)
        if (fetched.isEmpty()) return
        val merged = ids.flatMap { id -> fetched[id] ?: current.filter { it.restaurantId == id } }
        reviews.clear()
        reviews.addAll(merged)
        attemptOrNull { localStore.saveReviews(merged) }
    }

    suspend fun refreshRestaurantReviews(restaurantId: String, force: Boolean = false) {
        val fetched = attemptOrNull {
            services.reviewRepository.revalidateAll(listOf(restaurantId), force)
        }?.get(restaurantId)

        if (fetched != null) {
            val updated = reviews.filterNot { it.restaurantId == restaurantId } + fetched
            reviews.clear()
            reviews.addAll(updated)
            attemptOrNull { localStore.saveReviews(updated) }
        } else if (reviews.none { it.restaurantId == restaurantId }) {
            // Fallback al cache si no hay informacion para este restaurante aun
            val cached = attemptOrNull { localStore.loadReviews() }
                .orEmpty()
                .filter { it.restaurantId == restaurantId }
            if (cached.isNotEmpty()) reviews.addAll(cached)
        }
    }

    LaunchedEffect(overlay, selectedRestaurantId, online) {
        val restaurantId = selectedRestaurantId ?: return@LaunchedEffect
        if (overlay == Overlay.ReviewsList || overlay == Overlay.Detail) {
            reviewsLastSyncTime = attemptOrNull {
                services.documentCache.fetchedAt(CacheKeys.reviews(restaurantId))
            }
            if (online) {
                refreshRestaurantReviews(restaurantId, force = true)
                reviewsLastSyncTime = attemptOrNull {
                    services.documentCache.fetchedAt(CacheKeys.reviews(restaurantId))
                }
            }
        }
    }

    suspend fun refreshMyReviews(force: Boolean = false) {
        attemptOrNull { services.reviewRepository.revalidateMyReviews(force) }?.let {
            myReviews = it
            myReviewsLastSyncTime = services.reviewRepository.myReviewsLastSync()
        }
    }

    suspend fun refreshCrowding(force: Boolean = false) {
        val fetched = attemptOrNull { services.occupancyRepository.revalidate(force) } ?: return
        // No pisamos un reporte propio que sigue en cola offline
        val merged = (fetched.keys + pendingCrowding().keys).associateWith { id ->
            fetched[id].orEmpty() + pendingCrowding()[id].orEmpty()
        }
        crowding.clear()
        crowding.putAll(merged)
        attemptOrNull { localStore.saveCrowding(merged) }
    }

    suspend fun flushOutbox(ignoreBackoff: Boolean = false) {
        // El perfil editado offline tambien se reintenta al reconectar (antes solo al abrir la app)
        attemptOrNull { services.profileSynchronizer.syncPending() }
        val result = attemptOrNull { services.outboxFlusher.flush(ignoreBackoff) }
        updatePending()
        if (result == null) return
        if (result.authExpired) {
            services.markSessionExpired()
            return
        }
        if (result.dropped > 0) {
            Toast.makeText(context, "Some changes made offline could not be saved", Toast.LENGTH_SHORT).show()
        }
        if (OutboxKind.SAVED in result.sentKinds || result.dropped > 0) {
            attemptOrNull { services.savedRepository.fetch() }?.let {
                val merged = applyPendingSaved(it)
                savedIds = merged
                attemptOrNull { localStore.saveSavedIds(merged) }
            }
        }
        if (OutboxKind.OCCUPANCY in result.sentKinds) refreshCrowding(force = true)
        if (OutboxKind.ANALYTICS in result.sentKinds && recentViewsPending == 0) refreshRecentViews()
        if (OutboxKind.REVIEW in result.sentKinds) {
            refreshReviews(force = true)
            refreshMyReviews(force = true)
            refreshRestaurants(force = true)
        }
        // Si algo quedo en cola, WorkManager lo termina en segundo plano con backoff aunque cierren la app
        if (result.remaining > 0) services.syncScheduler.scheduleOutboxSync()
    }

    suspend fun refreshRecentViews() {
        if (!services.connectivity.online.value || recentViewsSyncing) return
        recentViewsSyncing = true
        try {
            attemptOrNull { services.recentViewsRepository.fetch { localStore.pending() } }?.let {
                recentViewsUpdatedAt = it
            }
            updatePending()
        } finally {
            recentViewsSyncing = false
        }
    }

    suspend fun enqueueAndFlush(request: OutboxRequest) {
        services.outboxStore.enqueue(request)
        updatePending()
        if (services.connectivity.online.value) flushOutbox() else services.syncScheduler.scheduleOutboxSync()
    }

    suspend fun refreshStale(force: Boolean = false, showIndicator: Boolean = false) {
        // La guarda va ANTES del flush: antes un flush podia lanzar refreshes en paralelo a otro refresh en curso
        if (syncInFlight) return
        syncInFlight = true
        if (showIndicator) refreshing = true
        try {
            flushOutbox()
            coroutineScope {
                launch { refreshRestaurants(force) }
                launch { refreshMyReviews(force) }
                launch { refreshCatalog(force) }
            }
            coroutineScope {
                launch { refreshReviews(force) }
                launch { refreshCrowding(force) }
                launch { refreshRecentViews() }
            }

            // Se guarda la hora si hay internet y se trajeron los datos
            if (services.connectivity.online.value) {
                val now = System.currentTimeMillis()
                attemptOrNull { services.localStore.saveDocument(LocalStore.LAST_SYNC_KEY, "ok", now) }
                lastSyncTime = now
            }
        } finally {
            syncInFlight = false
            if (showIndicator) refreshing = false
        }
    }

    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (windowInfo.isWindowFocused && startupDone && services.connectivity.online.value) {
            scope.launch { refreshStale() }
        }
    }

    // Periodic TTL watcher: runs only while the app is in the foreground and online
    LaunchedEffect(online, startupDone, windowInfo.isWindowFocused) {
        if (!online || !startupDone || !windowInfo.isWindowFocused) return@LaunchedEffect
        while (true) {
            delay(60_000L)
            refreshStale(force = false)
        }
    }

    fun recordRecentSearch(name: String) {
        recentSearches = (listOf(name) + recentSearches.filter { it != name }).take(prefs.maxRecentSearches)
        val snapshot = recentSearches
        scope.launch { attemptOrNull { localStore.saveRecentSearches(snapshot) } }
    }

    suspend fun loadRemote() {
        coroutineScope {
            launch {
                val remoteProfile = services.profileSynchronizer.resolve(user.name)
                if (remoteProfile != null) {
                    profile = remoteProfile
                    surveyDone = true
                } else {
                    localStore.loadProfileRecord()?.let {
                        profile = it.profile
                        surveyDone = true
                    }
                }
                profileResolved = true
            }
            launch {
                refreshRestaurants()
                if (!restaurantsReachable) {
                    restaurants = applyRatingSummaries(
                        services.restaurantRepository.cached(),
                        services.reviewRepository.cachedSummaries()
                    )
                }
                restaurantsResolved = true
            }
            launch {
                refreshCatalog()
                if (!catalogReachable) {
                    catalog = services.catalogRepository.cached()
                }
                catalogResolved = true
            }
            launch {
                if (services.connectivity.online.value) flushOutbox()
                val remoteSaved = attemptOrNull { services.savedRepository.fetch() }
                if (remoteSaved != null) {
                    val merged = applyPendingSaved(remoteSaved)
                    savedIds = merged
                    attemptOrNull { localStore.saveSavedIds(merged) }
                } else {
                    savedIds = applyPendingSaved(localStore.loadSavedIds())
                }
            }
        }
        // Una lista vacia que SI llego del servidor no es un fallo; solo lo es no tener datos ni haber podido pedirlos
        loadFailed = (restaurants.isEmpty() && !restaurantsReachable) || (catalog.isEmpty && !catalogReachable)
        if (loadFailed) return
        coroutineScope {
            launch {
                refreshReviews()
                if (reviews.isEmpty()) reviews.addAll(localStore.loadReviews())
            }
            launch {
                refreshMyReviews()
                if (myReviews.isEmpty()) myReviews = services.reviewRepository.cachedMyReviews()
            }
            launch {
                refreshCrowding()
                if (crowding.isEmpty()) crowding.putAll(localStore.loadCrowding())
            }
            launch { refreshRecentViews() }
        }
        startupDone = true
    }

    LaunchedEffect(online) {
        if (online && startupDone) refreshStale()
    }

    // La pantalla de error de primer arranque se recupera sola al volver la conexion
    LaunchedEffect(online, loadFailed) {
        if (online && loadFailed) {
            loadFailed = false
            restaurantsResolved = false
            catalogResolved = false
            loadRemote()
        }
    }

    LaunchedEffect(user.id) {
        services.prepareLocalData()
        // Si quedaron datos locales de otra cuenta se purgan antes de mostrarlos o enviarlos con este token
        services.ensureDataOwner(user.id)
        savedHome = if (services.preferences.current.value.rememberHomeFilters) services.preferences.loadHomeFilters() else HomeFilterState()
        updatePending()
        recentSearches = localStore.loadRecentSearches()
        attemptOrNull { services.recentViewsRepository.prune() }
        recentViewsUpdatedAt = attemptOrNull { services.recentViewsRepository.syncedAt() }
        lastSyncTime = services.localStore.loadDocument(LocalStore.LAST_SYNC_KEY)?.fetchedAt
        loadRemote()
    }

    // Sensor GPS: se pide el permiso al terminar el onboarding y se comparte con las pantallas
    val userLocation = rememberUserLocation()
    LaunchedEffect(surveyDone) {
        if (surveyDone && !userLocation.hasPermission) userLocation.requestPermission()
    }

    if (loadFailed) {
        ConnectionErrorView(
            onRetry = {
                loadFailed = false
                restaurantsResolved = false
                catalogResolved = false
                scope.launch { loadRemote() }
            }
        )
        return
    }

    if (!profileResolved || !restaurantsResolved || !catalogResolved) {
        LoadingView()
        return
    }

    // Onboarding: encuesta a pantalla completa solo la primera vez
    if (!surveyDone) {
        // Proveemos el catalogo ANTES de dibujar la vista
        CompositionLocalProvider(LocalCatalog provides catalog) {
            Scaffold { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    SurveyView(
                        isOnboarding = true,
                        initialName = user.name,
                        onComplete = { answers ->
                            val completed = answers.copy(name = answers.name.ifBlank { user.name })
                            profile = completed
                            surveyDone = true
                            scope.launch {
                                services.profileSynchronizer.update(completed)
                                updatePending()
                                if (outboxPending > 0) services.syncScheduler.scheduleOutboxSync()
                            }
                        }
                    )
                }
            }
        }
        return
    }

    // Tocar un restaurante → primero el modal de ocupación, luego el detalle
    val requestDetail: (Restaurant) -> Unit = {
        pendingRestaurant = it
    }

    fun recordView(restaurantId: String) {
        val now = System.currentTimeMillis()
        scope.launch {
            attemptOrNull { services.recentViewsRepository.recordView(restaurantId, now) }
            enqueueAndFlush(services.recentViewsRepository.logRequest(restaurantId, now))
        }
    }

    fun removeRecentView(restaurantId: String) {
        scope.launch {
            attemptOrNull { services.recentViewsRepository.remove(restaurantId) }
            enqueueAndFlush(services.recentViewsRepository.removeRequest(restaurantId))
        }
    }

    fun clearRecentViews() {
        scope.launch {
            attemptOrNull { services.recentViewsRepository.clear() }
            enqueueAndFlush(services.recentViewsRepository.clearRequest())
        }
    }

    fun commitDetail(r: Restaurant) {
        recordView(r.id)
        analytics.onOccupancyChecked(r.id, crowding[r.id].orEmpty().isNotEmpty())
        selectedRestaurantId = r.id
        pendingRestaurant = null
        overlayStack.clear()
        overlayStack.add(Overlay.Detail)
    }

    fun toggleSaved(id: String) {
        val wasSaved = id in savedIds
        savedIds = if (wasSaved) savedIds - id else savedIds + id
        val snapshot = savedIds
        scope.launch {
            attemptOrNull { localStore.saveSavedIds(snapshot) }
            sections.onInteraction(
                AppSection.Favorites,
                if (wasSaved) SectionAction.FavoriteRemoved else SectionAction.FavoriteAdded,
                visitSection = AppSection.Details
            )
            enqueueAndFlush(
                if (wasSaved) services.savedRepository.removeRequest(id) else services.savedRepository.addRequest(id)
            )
        }
    }

    fun popOverlay() {
        overlayStack.removeLastOrNull()
    }

    fun closeAll() {
        overlayStack.clear()
    }

    // Se cambio para que cuando se le de click al back si se llego desde las reseñas, nos devuelva a las reseñas
    fun backFromDetail() {
        val prev = overlayStack.getOrNull(overlayStack.lastIndex - 1)
        if (prev == Overlay.Ranking || prev == Overlay.MyReviews || prev == Overlay.RecentlyViewed) popOverlay() else closeAll()
    }

    fun openDetailFromRanking(r: Restaurant) {
        recordView(r.id)
        analytics.onOccupancyChecked(r.id, crowding[r.id].orEmpty().isNotEmpty())
        selectedRestaurantId = r.id
        overlayStack.add(Overlay.Detail)
    }

    fun selectTab(tab: Int) {
        overlayStack.clear()
        pendingRestaurant = null
        currentTab = tab
    }

    val mapVisible = overlay == null && currentTab == MAP_TAB
    val favoritesVisible = overlay == null && currentTab == FAVORITES_TAB
    val detailsVisible = overlay == Overlay.Detail || overlay == Overlay.WriteReview || overlay == Overlay.ReviewsList || overlay == Overlay.PartnerReport
    SectionVisibilityEffect(AppSection.Map, mapVisible, sections)
    SectionVisibilityEffect(AppSection.Favorites, favoritesVisible, sections)
    SectionVisibilityEffect(AppSection.Details, detailsVisible, sections)
    LaunchedEffect(favoritesVisible) {
        if (favoritesVisible) refreshRecentViews()
    }

    BackHandler(enabled = pendingRestaurant != null || overlayStack.isNotEmpty() || currentTab != 0) {
        when {
            pendingRestaurant != null -> pendingRestaurant = null
            overlay == Overlay.Detail -> backFromDetail()
            overlayStack.isNotEmpty() -> popOverlay()
            else -> currentTab = 0 // Back desde otra pestana vuelve a Discover en vez de cerrar la app
        }
    }

    // Obtenemos las reseñas que estan en cache
    val cachedRestaurantReviews = selectedRestaurant
        ?.let { r -> reviews.filter { it.restaurantId == r.id } }
        .orEmpty()

    // Obtenemos las reseñas que estan en el Outbox
    var pendingReviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    LaunchedEffect(outboxPending, selectedRestaurant) {
        val reviewPath = selectedRestaurant?.let { "/restaurants/${it.id}/reviews" }
        val pendingEntries = if (reviewPath == null) emptyList() else attemptOrNull { services.localStore.pending() }.orEmpty().filter {
            it.request.kind == OutboxKind.REVIEW && it.request.path == reviewPath
        }

        pendingReviews = pendingEntries.mapNotNull { entry ->
            val body = entry.request.body ?: return@mapNotNull null
            try {
                val ratings = ReviewRatings(
                    taste = (body["taste"] as? Number)?.toInt() ?: 0,
                    attention = (body["attention"] as? Number)?.toInt() ?: 0,
                    price = (body["price"] as? Number)?.toInt() ?: 0,
                    options = (body["options"] as? Number)?.toInt() ?: 0
                )

                val rawDiets = body["dietary_options"] as? List<*>
                val dietaryOptions = rawDiets?.map { it.toString() } ?: emptyList()

                Review(
                    id = "pending_${entry.id}",
                    restaurantId = selectedRestaurant?.id ?: "",
                    author = profile.name.ifEmpty { user.name },
                    date = "Pending sync...",
                    ratings = ratings,
                    dietaryOptions = dietaryOptions,
                    comment = (body["comment"] as? String).orEmpty()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    val restaurantReviews = pendingReviews + cachedRestaurantReviews

    val mealRules = remember(catalog) { MealRules(catalog) }
    CompositionLocalProvider(LocalRestaurants provides restaurants, LocalCatalog provides catalog, LocalMealRules provides mealRules) {
        Scaffold(
            bottomBar = {
                if (overlay != Overlay.Survey) Column {
                    if (!online) OfflineBanner(outboxPending)
                    else if (outboxPending > 0) PendingSyncBanner(outboxPending)
                    NavigationBar(containerColor = Color.White) {
                        val unselectedColor = Color.Gray
                        val selectedColor = Color(0xFFE8440A) // Naranja de Campus Eats
                        val tabs = listOf(
                            Icons.Default.Home to "Discover",
                            Icons.Default.Search to "Search",
                            Icons.Default.Map to "Map",
                            Icons.Default.Bookmark to "Saved",
                            Icons.Default.Person to "Profile"
                        )
                        tabs.forEachIndexed { index, (icon, label) ->
                            val active = currentTab == index && overlay == null
                            NavigationBarItem(
                                selected = active,
                                onClick = { selectTab(index) },
                                icon = { Icon(icon, label, tint = if (active) selectedColor else unselectedColor) },
                                label = { Text(label, color = if (active) selectedColor else unselectedColor) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                val restaurant = selectedRestaurant
                when {
                    overlay == Overlay.Detail && restaurant != null -> DetailView(
                        restaurant = restaurant,
                        reviews = restaurantReviews,
                        crowdingReports = crowding[restaurant.id].orEmpty(),
                        isSaved = restaurant.id in savedIds,
                        distanceMeters = userLocation.location?.let { distanceMeters(it, restaurant) },
                        userLocation = userLocation.location,
                        onToggleSave = { toggleSaved(restaurant.id) },
                        onBack = ::backFromDetail,
                        onWriteReview = {
                            sections.onInteraction(AppSection.Details, SectionAction.ReviewWriteOpened)
                            overlayStack.add(Overlay.WriteReview)
                        },
                        onSeeReviews = {
                            sections.onInteraction(AppSection.Details, SectionAction.ReviewsOpened)
                            overlayStack.add(Overlay.ReviewsList)
                        },
                        onPartnerReport = { overlayStack.add(Overlay.PartnerReport) },
                        isOnline = online,
                        onGetDirections = { tags ->
                            sections.onInteraction(AppSection.Details, SectionAction.DirectionsOpened)
                            analytics.onGoDecision(restaurant.id)
                            analytics.onTagClick(tags)
                        }
                    )

                    overlay == Overlay.WriteReview && restaurant != null -> WriteReviewView(
                        restaurant = restaurant,
                        authorName = profile.name,
                        onSubmit = { review ->
                            scope.launch {
                                if (!services.connectivity.online.value) {
                                    Toast.makeText(
                                        context,
                                        "You're offline. Your review will be posted when you reconnect.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                enqueueAndFlush(services.reviewRepository.submitRequest(review))
                            }
                        },
                        onBack = ::popOverlay,
                        isOnline = online
                    )

                    overlay == Overlay.ReviewsList && restaurant != null -> ReviewsListView(
                        restaurant = restaurant,
                        reviews = restaurantReviews,
                        isOnline = online,
                        lastSyncTime = reviewsLastSyncTime ?: lastSyncTime,
                        onBack = ::popOverlay,
                        onWriteReview = {
                            popOverlay()
                            overlayStack.add(Overlay.WriteReview)
                        }
                    )

                    overlay == Overlay.PartnerReport && restaurant != null -> PartnerReportView(
                        restaurant = restaurant,
                        loadReport = { id, days, force ->
                            services.partnerReportRepository.load(id, days, user.id, force)
                        },
                        onBack = ::popOverlay,
                        isOnline = online
                    )

                    overlay == Overlay.Ranking -> {
                        val rankingViewModel: RankingViewModel = viewModel(factory = RankingViewModel.factory(services))
                        val rankingState by rankingViewModel.state.collectAsStateWithLifecycle()
                        RankingView(
                            state = rankingState,
                            onCriterion = rankingViewModel::selectCriterion,
                            onWindow = rankingViewModel::selectWindow,
                            onRefresh = rankingViewModel::refresh,
                            onSelect = ::openDetailFromRanking,
                            onBack = ::popOverlay
                        )
                    }

                    overlay == Overlay.Settings -> {
                        val objectStats by services.objectMemoryCache.stats.collectAsStateWithLifecycle()
                        val documentStats by services.documentMemoryCache.stats.collectAsStateWithLifecycle()
                        var diskRefresh by remember { mutableIntStateOf(0) }
                        val disk by produceState(AppServices.DiskCacheReport(0, 0L), diskRefresh) {
                            value = services.diskCacheReport()
                        }
                        SettingsView(
                            preferences = prefs,
                            onChange = { transform -> scope.launch { services.preferences.update(transform) } },
                            objectCache = objectStats,
                            documentCache = documentStats,
                            diskCache = disk,
                            pendingChanges = outboxPending,
                            online = online,
                            onSyncNow = { scope.launch { flushOutbox(ignoreBackoff = true) } },
                            onClearCache = {
                                scope.launch {
                                    services.clearAllCaches()
                                    diskRefresh++
                                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = ::popOverlay
                        )
                    }

                    overlay == Overlay.MyReviews -> {
                        val pendingRestaurantIds = pendingMyReviews.map { it.restaurantId }.toSet()
                        val combinedMyReviews = pendingMyReviews + myReviews.filterNot { it.restaurantId in pendingRestaurantIds }
                        MyReviewsView(
                            reviews = combinedMyReviews,
                            restaurantsById = restaurants.associateBy { it.id },
                            isOnline = online,
                            lastSyncTime = myReviewsLastSyncTime ?: lastSyncTime,
                            isLoading = myReviewsLoading && combinedMyReviews.isEmpty(),
                            onSelectRestaurant = ::openDetailFromRanking,
                            onBack = ::popOverlay
                        )
                    }

                    overlay == Overlay.RecentlyViewed -> {
                        LaunchedEffect(Unit) { refreshRecentViews() }
                        RecentlyViewedView(
                            entries = resolveRecentlyViewed(recentViews, restaurants),
                            updatedAt = recentViewsUpdatedAt,
                            isOnline = online,
                            pendingChanges = recentViewsPending,
                            isSyncing = recentViewsSyncing,
                            onRemove = { removeRecentView(it.id) },
                            onSelect = {
                                sections.onInteraction(AppSection.Favorites, SectionAction.SavedItemSelected)
                                openDetailFromRanking(it)
                            },
                            onClear = ::clearRecentViews,
                            onBack = ::popOverlay
                        )
                    }

                    overlay == Overlay.Survey -> SurveyView(
                        initialName = profile.name,
                        onComplete = { answers ->
                            val updated = answers.copy(name = answers.name.ifBlank { user.name })
                            analytics.onProfileSaved(profile, updated)
                            profile = updated
                            scope.launch {
                                services.profileSynchronizer.update(updated)
                                updatePending()
                                if (outboxPending > 0) services.syncScheduler.scheduleOutboxSync()
                            }
                            popOverlay()
                        }
                    )

                    else -> when (currentTab) {
                        0 -> HomeView(
                            onSelect = requestDetail,
                            onOpenProfile = { selectTab(PROFILE_TAB) },
                            onOpenRanking = { overlayStack.add(Overlay.Ranking) },
                            profile = profile,
                            crowding = crowding,
                            savedIds = savedIds,
                            reviews = reviews,
                            userLocation = userLocation.location,
                            initialCategory = savedHome.category,
                            initialFilters = HomeFilters(
                                openNow = savedHome.openNow,
                                savedOnly = savedHome.savedOnly,
                                prices = savedHome.prices,
                                minRating = savedHome.minRating
                            ),
                            onViewStateChanged = { category, filters ->
                                val state = HomeFilterState(category, filters.openNow, filters.savedOnly, filters.prices, filters.minRating)
                                savedHome = state
                                if (prefs.rememberHomeFilters) scope.launch { services.preferences.saveHomeFilters(state) }
                            },
                            onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Home) },
                            onFilterSheetOpened = { sections.onSectionShown(AppSection.Filters) },
                            onFilterSheetClosed = { sections.onSectionLeft(AppSection.Filters) },
                            onFiltersApplied = {
                                sections.onInteraction(AppSection.Filters, SectionAction.FiltersApplied)
                            },
                            onFiltersCleared = {
                                sections.onInteraction(AppSection.Filters, SectionAction.FiltersCleared)
                            },
                            isRefreshing = refreshing,
                            onRefresh = {
                                // Para que sea mas informativo verificamos la conexion antes de forzar la recarga
                                if (services.connectivity.online.value) {
                                    scope.launch { refreshStale(force = true, showIndicator = true) }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "You are offline. Showing saved data.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            isOnline = online,
                            lastSyncTime = lastSyncTime
                        )
                        1 -> SearchView(
                            onSelect = {
                                recordRecentSearch(it.name)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            userLocation = userLocation.location,
                            onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Search) },
                            recentSearches = recentSearches,
                            onClearRecent = {
                                recentSearches = emptyList()
                                scope.launch { attemptOrNull { localStore.saveRecentSearches(emptyList()) } }
                            }
                        )
                        MAP_TAB -> MapView(
                            onSelect = {
                                sections.onInteraction(AppSection.Map, SectionAction.RestaurantSelected)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            userLocation = userLocation.location,
                            hasLocationPermission = userLocation.hasPermission,
                            onRetryLocation = userLocation.requestPermission,
                            onFilterUsed = {
                                sections.onInteraction(AppSection.Map, SectionAction.FilterChipChanged)
                                analytics.onFilterUsed(it, AnalyticsScreen.Map)
                            },
                            onMarkerSelected = {
                                sections.onInteraction(AppSection.Map, SectionAction.MarkerSelected)
                            },
                            isOnline = online
                        )
                        FAVORITES_TAB -> SavedView(
                            onSelect = {
                                sections.onInteraction(AppSection.Favorites, SectionAction.SavedItemSelected)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            savedIds = savedIds,
                            userLocation = userLocation.location,
                            recentlyViewed = resolveRecentlyViewed(recentViews, restaurants),
                            onSeeAllRecent = { overlayStack.add(Overlay.RecentlyViewed) }
                        )
                        PROFILE_TAB -> ProfileView(
                            profile = profile,
                            userEmail = user.email,
                            showLogoutWarning = auth.showLogoutWarning.collectAsStateWithLifecycle().value,
                            onRetakeSurvey = { overlayStack.add(Overlay.Survey) },
                            onOpenSettings = { overlayStack.add(Overlay.Settings) },
                            onOpenMyReviews = { overlayStack.add(Overlay.MyReviews) },
                            onLogoutAttempt = { auth.attemptLogout() },
                            // Un solo camino de logout: el ViewModel envia lo pendiente y purga TODO (ver AppServices.purgeUserData)
                            onLogoutConfirm = { auth.confirmLogout() },
                            onLogoutCancel = { auth.cancelLogout() },
                            onBack = { selectTab(0) }
                        )
                    }
                }

                // Modal de ocupación: flota sobre la tab antes de abrir el detalle
                pendingRestaurant?.let { pending ->
                    if (overlay == null) {
                        CrowdingModal(
                            restaurant = pending,
                            onSubmit = { level ->
                                scope.launch {
                                    crowding[pending.id] = crowding[pending.id].orEmpty() + level
                                    attemptOrNull { localStore.saveCrowding(crowding.toMap()) }
                                    enqueueAndFlush(services.occupancyRepository.reportRequest(pending.id, level))
                                }
                                commitDetail(pending)
                            },
                            onSkip = { commitDetail(pending) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionVisibilityEffect(section: AppSection, visible: Boolean, sections: SectionAnalyticsViewModel) {
    LaunchedEffect(visible) {
        if (visible) sections.onSectionShown(section) else sections.onSectionLeft(section)
    }
    DisposableEffect(Unit) {
        onDispose { sections.onSectionLeft(section) }
    }
}