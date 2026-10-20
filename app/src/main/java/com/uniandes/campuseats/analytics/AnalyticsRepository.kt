package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.data.Profile

class AnalyticsRepository(
    private val gateway: AnalyticsGateway,
    private val installId: () -> String
) {
    fun logFilterUsed(filter: String, screen: AnalyticsScreen) {
        gateway.send(FILTER_USAGE_PATH, mapOf("filter" to filter, "screen" to screen.wireName))
    }

    fun logOccupancyChecked(restaurantId: String, screen: AnalyticsScreen) {
        logOccupancy(restaurantId, screen, ACTION_CHECKED)
    }

    fun logOccupancySelected(restaurantId: String, screen: AnalyticsScreen) {
        logOccupancy(restaurantId, screen, ACTION_SELECTED)
    }

    fun logProfileEdits(old: Profile, updated: Profile) {
        val changedFields = ProfileDiff.changedFields(old, updated)
        if (changedFields.isEmpty()) return
        val id = installId()
        changedFields.forEach { field ->
            gateway.send(PROFILE_EDIT_PATH, mapOf("install_id" to id, "field" to field))
        }
    }

    private fun logOccupancy(restaurantId: String, screen: AnalyticsScreen, action: String) {
        gateway.send(
            OCCUPANCY_PATH,
            mapOf("restaurant_id" to restaurantId, "screen" to screen.wireName, "action" to action)
        )
    }

    companion object {
        const val FILTER_USAGE_PATH = "/analytics/filter-usage"
        const val OCCUPANCY_PATH = "/analytics/occupancy-check"
        const val PROFILE_EDIT_PATH = "/analytics/profile-edit"
        const val ACTION_CHECKED = "checked"
        const val ACTION_SELECTED = "selected"
    }
}
