package com.uniandes.campuseats.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uniandes.campuseats.AppServices
import com.uniandes.campuseats.data.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {

    fun onFilterUsed(filter: String, screen: AnalyticsScreen) {
        repository.logFilterUsed(filter, screen)
    }

    fun onOccupancyChecked(restaurantId: String, hasData: Boolean) {
        repository.logOccupancyChecked(restaurantId, hasData)
    }

    fun onGoDecision(restaurantId: String) {
        repository.logGoDecision(restaurantId)
    }

    fun onProfileSaved(old: Profile, updated: Profile) {
        repository.logProfileEdits(old, updated)
    }

    fun onTagClick(tags: List<String>) {
        repository.logTagClicks(tags)
    }

    companion object {
        fun factory(services: AppServices): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val gateway = AnalyticsGateway(
                    services.analyticsTransport,
                    CoroutineScope(SupervisorJob() + Dispatchers.IO)
                )
                AnalyticsViewModel(AnalyticsRepository(gateway) { InstallId.get(services.appContext) })
            }
        }
    }
}
