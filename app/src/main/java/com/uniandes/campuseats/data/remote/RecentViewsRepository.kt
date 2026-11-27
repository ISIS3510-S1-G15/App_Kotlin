package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.outbox.OutboxEntry
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class RecentView(
    val restaurantId: String,
    val viewCount: Int,
    val lastViewedAt: Long
)

class RecentViewsRepository(
    private val api: RemoteApi,
    private val store: LocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val migrationLock = Mutex()
    private var migrated = false

    fun observe(): Flow<List<RecentView>> = store.observeRecentViews()
        .onStart { prepare() }
        .catch { emit(emptyList()) }

    suspend fun syncedAt(): Long? {
        prepare()
        return store.recentViewsSyncedAt()
    }

    suspend fun fetch(pending: suspend () -> List<OutboxEntry>): Long {
        prepare()
        val json = api.get("$RECENT_VIEWS_PATH?limit=$MAX_ITEMS")
        val remote = parseRemote(json.optJSONArray("items") ?: JSONArray())
        val merged = applyPending(remote, pending().filter { isRecentViewsRequest(it.request) })
        val now = clock()
        store.replaceRecentViews(merged, now, MAX_ITEMS, MAX_AGE_MS)
        return now
    }

    suspend fun recordView(restaurantId: String, at: Long) {
        prepare()
        store.recordRecentView(restaurantId, at, MAX_ITEMS, MAX_AGE_MS)
    }

    suspend fun remove(restaurantId: String) {
        store.removeRecentView(restaurantId)
    }

    suspend fun clear() {
        store.clearRecentViews()
    }

    suspend fun prune() {
        prepare()
        store.pruneRecentViews(MAX_ITEMS, MAX_AGE_MS)
    }

    fun logRequest(restaurantId: String, at: Long) = OutboxRequest(
        OutboxKind.ANALYTICS,
        "POST",
        LOG_VIEW_PATH,
        mapOf("restaurant_id" to restaurantId, "viewed_at" to formatIso(at))
    )

    fun removeRequest(restaurantId: String) =
        OutboxRequest(OutboxKind.ANALYTICS, "DELETE", "$RECENT_VIEWS_PATH/$restaurantId")

    fun clearRequest() = OutboxRequest(OutboxKind.ANALYTICS, "DELETE", RECENT_VIEWS_PATH)

    fun isRecentViewsRequest(request: OutboxRequest): Boolean =
        request.kind == OutboxKind.ANALYTICS &&
            (request.path == LOG_VIEW_PATH || request.path == RECENT_VIEWS_PATH || request.path.startsWith("$RECENT_VIEWS_PATH/"))

    private suspend fun prepare() {
        if (migrated) return
        migrationLock.withLock {
            if (migrated) return
            store.migrateLegacyRecentViews()
            migrated = true
        }
    }

    private fun applyPending(base: List<RecentView>, pending: List<OutboxEntry>): List<RecentView> {
        var result = base
        pending.map { it.request }.forEach { request ->
            when {
                request.method == "DELETE" && request.path == RECENT_VIEWS_PATH -> result = emptyList()
                request.method == "DELETE" && request.path.startsWith("$RECENT_VIEWS_PATH/") -> {
                    val id = request.path.removePrefix("$RECENT_VIEWS_PATH/")
                    result = result.filter { it.restaurantId != id }
                }
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
        return listOf(updated) + current.filter { it.restaurantId != restaurantId }
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

    companion object {
        const val RECENT_VIEWS_PATH = "/me/recent-views"
        const val LOG_VIEW_PATH = "/analytics/restaurant-view"
        const val MAX_ITEMS = 50
        const val MAX_AGE_DAYS = 30
        const val MAX_AGE_MS = MAX_AGE_DAYS * 24L * 60L * 60L * 1000L

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
