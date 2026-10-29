package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.cache.DocumentCache
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
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

class OccupancyRepository(
    private val api: RemoteApi,
    private val cache: DocumentCache,
    private val policy: CachePolicy = CachePolicy()
) {

    suspend fun fetchAll(): Map<String, List<Int>> {
        val items = api.get("/occupancy").optJSONArray("items") ?: return emptyMap()
        val parsed = (0 until items.length())
            .map { items.getJSONObject(it) }
            .associate { it.getString("restaurant_id") to reportsOf(it) }
        policy.markFetched(cache, CacheKeys.OCCUPANCY)
        return parsed
    }

    suspend fun revalidate(force: Boolean = false): Map<String, List<Int>>? {
        val fresh = !force && policy.isFreshOrFalse(cache, CacheKeys.OCCUPANCY, CacheTtl.OCCUPANCY)
        return if (fresh) null else fetchAll()
    }

    fun reportRequest(restaurantId: String, appLevel: Int) = OutboxRequest(
        OutboxKind.OCCUPANCY,
        "POST",
        "/occupancy/reports",
        mapOf("restaurant_id" to restaurantId, "level" to remoteLevelFromApp(appLevel))
    )

    suspend fun report(restaurantId: String, appLevel: Int): List<Int> {
        val reports = reportsOf(
            api.post(
                "/occupancy/reports",
                mapOf("restaurant_id" to restaurantId, "level" to remoteLevelFromApp(appLevel))
            )
        )
        attemptOrNull { cache.clear(CacheKeys.OCCUPANCY) }
        return reports
    }
}
