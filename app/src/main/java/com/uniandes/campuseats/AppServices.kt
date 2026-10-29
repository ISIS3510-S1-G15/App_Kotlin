package com.uniandes.campuseats

import android.content.Context
import com.uniandes.campuseats.analytics.AnalyticsTransport
import com.uniandes.campuseats.analytics.HttpAnalyticsTransport
import com.uniandes.campuseats.auth.AuthRepository
import com.uniandes.campuseats.auth.SessionStore
import com.uniandes.campuseats.data.local.AppDatabase
import com.uniandes.campuseats.data.local.LegacyPreferencesMigration
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.local.RoomCatalogCache
import com.uniandes.campuseats.data.local.RoomRestaurantCache
import com.uniandes.campuseats.data.remote.CatalogRepository
import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.ProfileRepository
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.SavedRepository
import com.uniandes.campuseats.net.HttpRemoteApi
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppServices private constructor(val appContext: Context) {
    val sessionStore = SessionStore(appContext)
    val localStore = LocalStore(AppDatabase.create(appContext))
    private val migrationLock = Mutex()
    val api: RemoteApi = HttpRemoteApi(BuildConfig.API_BASE_URL) { sessionStore.token }
    val authRepository = AuthRepository(api, sessionStore)
    val profileRepository = ProfileRepository(api)
    val savedRepository = SavedRepository(api)
    val restaurantRepository = RestaurantRepository(api, RoomRestaurantCache(localStore))
    val reviewRepository = ReviewRepository(api)
    val catalogRepository = CatalogRepository(api, RoomCatalogCache(localStore))
    val occupancyRepository = OccupancyRepository(api)
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
