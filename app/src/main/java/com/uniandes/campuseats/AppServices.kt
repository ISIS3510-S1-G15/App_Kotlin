package com.uniandes.campuseats

import android.content.Context
import com.uniandes.campuseats.analytics.AnalyticsTransport
import com.uniandes.campuseats.analytics.HttpAnalyticsTransport
import com.uniandes.campuseats.auth.AuthRepository
import com.uniandes.campuseats.auth.SessionStore
import com.uniandes.campuseats.data.local.AppDatabase
import com.uniandes.campuseats.data.local.LegacyPreferencesMigration
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.local.RoomDocumentCache
import com.uniandes.campuseats.data.local.HomeFilterStore
import com.uniandes.campuseats.data.outbox.OutboxFlusher
import com.uniandes.campuseats.data.outbox.OutboxStore
import com.uniandes.campuseats.data.remote.CatalogRepository
import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.ProfileRepository
import com.uniandes.campuseats.data.remote.ProfileSynchronizer
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.SavedRepository
import com.uniandes.campuseats.net.ConnectivityMonitor
import com.uniandes.campuseats.net.ConnectivityObserver
import com.uniandes.campuseats.net.HttpRemoteApi
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppServices private constructor(val appContext: Context) {
    val sessionStore = SessionStore(appContext)
    val localStore = LocalStore(AppDatabase.create(appContext))
    val documentCache = RoomDocumentCache(localStore)
    val homeFilterStore = HomeFilterStore(appContext)
    private val migrationLock = Mutex()
    val api: RemoteApi = HttpRemoteApi(BuildConfig.API_BASE_URL) { sessionStore.token }
    val authRepository = AuthRepository(api, sessionStore)
    val connectivity: ConnectivityMonitor = ConnectivityObserver(appContext)
    val outboxStore: OutboxStore = localStore
    val outboxFlusher = OutboxFlusher(api, outboxStore)
    val profileRepository = ProfileRepository(api)
    val profileSynchronizer = ProfileSynchronizer(localStore, profileRepository)
    val savedRepository = SavedRepository(api)
    val restaurantRepository = RestaurantRepository(api, documentCache)
    val reviewRepository = ReviewRepository(api, documentCache)
    val catalogRepository = CatalogRepository(api, documentCache)
    val occupancyRepository = OccupancyRepository(api, documentCache)
    val analyticsTransport: AnalyticsTransport = HttpAnalyticsTransport(api)

    suspend fun prepareLocalData() = migrationLock.withLock {
        LegacyPreferencesMigration(appContext, localStore).runOnce()
    }

    companion object {
        @Volatile
        private var instance: AppServices? = null

        fun get(context: Context): AppServices = instance ?: synchronized(this) {
            instance ?: AppServices(context.applicationContext).also { instance = it }
        }
    }
}
