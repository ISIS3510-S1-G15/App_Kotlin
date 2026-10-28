package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.data.Profile

class AnalyticsRepository(
    private val gateway: AnalyticsGateway,
    private val installId: () -> String
) {
    fun logFilterUsed(filter: String, screen: AnalyticsScreen) {
        gateway.send(FILTER_USAGE_PATH, mapOf("filter" to filter, "screen" to screen.wireName))
    }

    fun logOccupancyChecked(restaurantId: String, hasData: Boolean) {
        gateway.send(
            OCCUPANCY_PATH,
            mapOf("restaurant_id" to restaurantId, "event" to EVENT_CHECKED, "has_data" to hasData)
        )
    }

    fun logGoDecision(restaurantId: String) {
        gateway.send(OCCUPANCY_PATH, mapOf("restaurant_id" to restaurantId, "event" to EVENT_GO_DECISION))
    }

    fun logProfileEdits(old: Profile, updated: Profile) {
        val changedFields = ProfileDiff.changedFields(old, updated)
        if (changedFields.isEmpty()) return
        val id = installId()
        changedFields.forEach { field ->
            gateway.send(PROFILE_EDIT_PATH, mapOf("install_id" to id, "field" to field))
        }
    }

    fun logTagClicks(tags: List<String>) {
        if (tags.isEmpty()) return
        gateway.send(TAG_CLICK_PATH, mapOf("tags" to tags))
    }

    companion object {
        const val FILTER_USAGE_PATH = "/analytics/filter-usage"
        const val OCCUPANCY_PATH = "/analytics/occupancy-event"
        const val PROFILE_EDIT_PATH = "/analytics/profile-edit"
        const val TAG_CLICK_PATH = "/analytics/tag-click"
        const val EVENT_CHECKED = "occupancy_checked"
        const val EVENT_GO_DECISION = "go_decision"
    }
}
