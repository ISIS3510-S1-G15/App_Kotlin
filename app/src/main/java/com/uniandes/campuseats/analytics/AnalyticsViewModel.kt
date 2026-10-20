package com.uniandes.campuseats.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uniandes.campuseats.BuildConfig
import com.uniandes.campuseats.data.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {

    fun onFilterUsed(filter: String, screen: AnalyticsScreen) {
        repository.logFilterUsed(filter, screen)
    }

    fun onRestaurantChecked(restaurantId: String, tab: Int) {
        repository.logOccupancyChecked(restaurantId, AnalyticsScreen.fromTab(tab))
    }

    fun onRestaurantSelected(restaurantId: String, tab: Int) {
        repository.logOccupancySelected(restaurantId, AnalyticsScreen.fromTab(tab))
    }

    fun onProfileSaved(old: Profile, updated: Profile) {
        repository.logProfileEdits(old, updated)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    val gateway = AnalyticsGateway(
                        HttpAnalyticsTransport(BuildConfig.ANALYTICS_BASE_URL),
                        CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    )
                    AnalyticsViewModel(AnalyticsRepository(gateway) { InstallId.get(appContext) })
                }
            }
        }
    }
}
