package com.uniandes.campuseats.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uniandes.campuseats.AppServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SectionAnalyticsViewModel(
    private val tracker: SectionAnalyticsTracker,
    private val scope: CoroutineScope? = null
) : ViewModel() {

    fun onSectionShown(section: AppSection) = tracker.onSectionShown(section)

    fun onSectionLeft(section: AppSection) = tracker.onSectionLeft(section)

    fun onInteraction(section: AppSection, action: SectionAction, visitSection: AppSection = section) =
        tracker.onInteraction(section, action, visitSection)

    override fun onCleared() {
        scope?.cancel()
    }

    companion object {
        fun factory(services: AppServices): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                SectionAnalyticsViewModel(SectionAnalyticsTracker(services.analyticsTransport, scope), scope)
            }
        }
    }
}
