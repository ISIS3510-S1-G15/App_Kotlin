package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONObject

fun appLevelFromRemote(remoteLevel: Int): Int = when (remoteLevel) {
    0 -> 1
    1 -> 3
    else -> 5
}

fun remoteLevelFromApp(appLevel: Int): Int = when {
    appLevel <= 2 -> 0
    appLevel == 3 -> 1
    else -> 2
}

private fun reportsOf(json: JSONObject): List<Int> {
    val reports = json.optJSONArray("reports") ?: return emptyList()
    return (0 until reports.length()).map { appLevelFromRemote(reports.getInt(it)) }
}

class OccupancyRepository(private val api: RemoteApi) {

    suspend fun fetchAll(): Map<String, List<Int>> {
        val items = api.get("/occupancy").optJSONArray("items") ?: return emptyMap()
        return (0 until items.length())
            .map { items.getJSONObject(it) }
            .associate { it.getString("restaurant_id") to reportsOf(it) }
    }

    suspend fun report(restaurantId: String, appLevel: Int): List<Int> = reportsOf(
        api.post(
            "/occupancy/reports",
            mapOf("restaurant_id" to restaurantId, "level" to remoteLevelFromApp(appLevel))
        )
    )
}
