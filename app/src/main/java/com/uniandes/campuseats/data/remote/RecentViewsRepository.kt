package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.outbox.OutboxEntry
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class RecentView(
    val restaurantId: String,
    val viewCount: Int,
    val lastViewedAt: Long
)

data class RecentViewsSnapshot(
    val items: List<RecentView>,
    val updatedAt: Long?
)

class RecentViewsRepository(
    private val api: RemoteApi,
    private val store: LocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun cached(): RecentViewsSnapshot {
        val doc = store.loadDocument(LocalStore.RECENT_VIEWS_KEY) ?: return RecentViewsSnapshot(emptyList(), null)
        val items = runCatching { decode(JSONArray(doc.payload)) }.getOrDefault(emptyList())
        return RecentViewsSnapshot(items, doc.fetchedAt)
    }

    suspend fun fetch(pending: List<OutboxEntry>): RecentViewsSnapshot {
        val json = api.get("$RECENT_VIEWS_PATH?limit=$FETCH_LIMIT")
        val remote = parseRemote(json.optJSONArray("items") ?: JSONArray())
        val merged = applyPending(remote, pending)
        val now = clock()
        save(merged, now)
        return RecentViewsSnapshot(merged, now)
    }

    suspend fun recordView(current: List<RecentView>, restaurantId: String, at: Long): RecentViewsSnapshot {
        val updated = increment(current, restaurantId, at)
        save(updated, at)
        return RecentViewsSnapshot(updated, at)
    }

    suspend fun clearLocal(): RecentViewsSnapshot {
        val now = clock()
        save(emptyList(), now)
        return RecentViewsSnapshot(emptyList(), now)
    }

    fun logRequest(restaurantId: String, at: Long) = OutboxRequest(
        OutboxKind.ANALYTICS,
        "POST",
        LOG_VIEW_PATH,
        mapOf("restaurant_id" to restaurantId, "viewed_at" to formatIso(at))
    )

    fun clearRequest() = OutboxRequest(OutboxKind.ANALYTICS, "DELETE", RECENT_VIEWS_PATH)

    private fun applyPending(base: List<RecentView>, pending: List<OutboxEntry>): List<RecentView> {
        var result = base
        pending.map { it.request }.forEach { request ->
            when {
                request.method == "DELETE" && request.path == RECENT_VIEWS_PATH -> result = emptyList()
                request.method == "POST" && request.path == LOG_VIEW_PATH -> {
                    val body = request.body ?: return@forEach
                    val id = body["restaurant_id"]?.toString() ?: return@forEach
                    val at = (body["viewed_at"] as? String)?.let(::parseIso) ?: clock()
                    result = increment(result, id, at)
                }
            }
        }
        return result
    }

    private fun increment(current: List<RecentView>, restaurantId: String, at: Long): List<RecentView> {
        val existing = current.firstOrNull { it.restaurantId == restaurantId }
        val updated = RecentView(
            restaurantId = restaurantId,
            viewCount = (existing?.viewCount ?: 0) + 1,
            lastViewedAt = maxOf(existing?.lastViewedAt ?: 0L, at)
        )
        return (listOf(updated) + current.filter { it.restaurantId != restaurantId })
            .sortedByDescending { it.lastViewedAt }
            .take(FETCH_LIMIT)
    }

    private suspend fun save(items: List<RecentView>, at: Long) {
        store.saveDocument(LocalStore.RECENT_VIEWS_KEY, encode(items).toString(), at)
    }

    private fun parseRemote(array: JSONArray): List<RecentView> = (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("restaurant_id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        RecentView(
            restaurantId = id,
            viewCount = item.optInt("view_count", 1),
            lastViewedAt = parseIso(item.optString("last_viewed_at")) ?: clock()
        )
    }

    private fun encode(items: List<RecentView>): JSONArray = JSONArray().apply {
        items.forEach {
            put(
                JSONObject()
                    .put("restaurant_id", it.restaurantId)
                    .put("view_count", it.viewCount)
                    .put("last_viewed_at", it.lastViewedAt)
            )
        }
    }

    private fun decode(array: JSONArray): List<RecentView> = (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        RecentView(
            restaurantId = item.optString("restaurant_id"),
            viewCount = item.optInt("view_count", 1),
            lastViewedAt = item.optLong("last_viewed_at")
        )
    }.filter { it.restaurantId.isNotBlank() }

    companion object {
        const val RECENT_VIEWS_PATH = "/me/recent-views"
        const val LOG_VIEW_PATH = "/analytics/restaurant-view"
        private const val FETCH_LIMIT = 50

        fun formatIso(millis: Long): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(millis)

        fun parseIso(raw: String?): Long? {
            if (raw.isNullOrBlank() || raw.length < 19) return null
            val base = runCatching {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw.substring(0, 19).replace(' ', 'T'))?.time
            }.getOrNull() ?: return null
            var rest = raw.substring(19)
            var fraction = 0L
            if (rest.startsWith(".")) {
                val digits = rest.drop(1).takeWhile { it.isDigit() }
                fraction = digits.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
                rest = rest.drop(1 + digits.length)
            }
            val offset = when {
                rest.isEmpty() || rest == "Z" -> 0L
                rest.length >= 6 && (rest[0] == '+' || rest[0] == '-') -> {
                    val sign = if (rest[0] == '-') -1 else 1
                    val hours = rest.substring(1, 3).toLongOrNull() ?: 0L
                    val minutes = rest.substring(4, 6).toLongOrNull() ?: 0L
                    sign * (hours * 3_600_000L + minutes * 60_000L)
                }
                else -> 0L
            }
            return base + fraction - offset
        }
    }
}
