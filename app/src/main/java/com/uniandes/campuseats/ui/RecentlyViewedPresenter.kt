package com.uniandes.campuseats.ui

import androidx.compose.runtime.Immutable
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.currentMinutesOfDay
import com.uniandes.campuseats.data.isOpenAt
import com.uniandes.campuseats.data.remote.RecentView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class RecentlyViewedEntry(
    val restaurant: Restaurant,
    val view: RecentView,
    val isOpen: Boolean,
    val subtitle: String,
    val viewedLabel: String,
    val dayLabel: String
)

@Immutable
sealed interface RecentlyViewedRow {
    val key: String
    val contentType: Int

    @Immutable
    data class Header(val label: String) : RecentlyViewedRow {
        override val key: String = "header_$label"
        override val contentType: Int get() = HEADER_TYPE
    }

    @Immutable
    data class Item(val entry: RecentlyViewedEntry) : RecentlyViewedRow {
        override val key: String = "item_${entry.restaurant.id}"
        override val contentType: Int get() = ITEM_TYPE
    }

    companion object {
        const val HEADER_TYPE = 0
        const val ITEM_TYPE = 1
    }
}

@Immutable
data class RecentlyViewedUi(
    val entries: List<RecentlyViewedEntry>,
    val rows: List<RecentlyViewedRow>,
    val byId: Map<String, RecentlyViewedEntry>
) {
    companion object {
        val Empty = RecentlyViewedUi(emptyList(), emptyList(), emptyMap())
    }
}

private val clockFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val dayFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH)
}

fun formatClock(millis: Long): String = clockFormat.get()!!.format(Date(millis))

object RecentlyViewedPresenter {

    const val PREFETCH_PARALLELISM = 3

    private class Snapshot(
        val views: List<RecentView>,
        val restaurants: List<Restaurant>,
        val today: Long,
        val minutesOfDay: Int,
        val ui: RecentlyViewedUi
    )

    @Volatile
    private var last: Snapshot? = null

    suspend fun build(
        views: List<RecentView>,
        restaurants: List<Restaurant>,
        now: Long = System.currentTimeMillis(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): RecentlyViewedUi {
        val minutesOfDay = currentMinutesOfDay()
        val today = startOfDay(Calendar.getInstance(), now)
        last?.let { s ->
            if (s.views === views && s.restaurants === restaurants && s.today == today && s.minutesOfDay == minutesOfDay) {
                return s.ui
            }
        }
        val ui = withContext(dispatcher) { compute(views, restaurants, now, minutesOfDay) }
        last = Snapshot(views, restaurants, today, minutesOfDay, ui)
        return ui
    }

    suspend fun compute(
        views: List<RecentView>,
        restaurants: List<Restaurant>,
        now: Long,
        minutesOfDay: Int
    ): RecentlyViewedUi {
        if (views.isEmpty()) return RecentlyViewedUi.Empty
        val calendar = Calendar.getInstance()
        val today = startOfDay(calendar, now)
        calendar.timeInMillis = today
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis
        val clock = clockFormat.get()!!
        val days = dayFormat.get()!!
        val date = Date()
        val byId = HashMap<String, Restaurant>(restaurants.size * 2)
        for (r in restaurants) byId[r.id] = r
        val sorted = if (isSortedDescending(views)) views else views.sortedByDescending { it.lastViewedAt }
        val context = currentCoroutineContext()
        val entries = ArrayList<RecentlyViewedEntry>(sorted.size)
        val rows = ArrayList<RecentlyViewedRow>(sorted.size + 8)
        val entriesById = HashMap<String, RecentlyViewedEntry>(sorted.size * 2)
        val olderLabels = HashMap<Long, String>()
        val text = StringBuilder(48)
        var currentLabel: String? = null
        for (view in sorted) {
            context.ensureActive()
            val restaurant = byId[view.restaurantId] ?: continue
            val at = view.lastViewedAt
            val label = when {
                at >= today -> "TODAY"
                at >= yesterday -> "YESTERDAY"
                else -> {
                    val dayStart = startOfDay(calendar, at)
                    olderLabels.getOrPut(dayStart) {
                        date.time = at
                        days.format(date).uppercase(Locale.ENGLISH)
                    }
                }
            }
            date.time = at
            text.setLength(0)
            text.append("Viewed ").append(view.viewCount).append(if (view.viewCount == 1) " time" else " times")
                .append(" · last at ").append(clock.format(date))
            val viewedLabel = text.toString()
            text.setLength(0)
            text.append(restaurant.category).append(" · ").append(restaurant.location)
            val entry = RecentlyViewedEntry(
                restaurant = restaurant,
                view = view,
                isOpen = isOpenAt(restaurant.hours, minutesOfDay),
                subtitle = text.toString(),
                viewedLabel = viewedLabel,
                dayLabel = label
            )
            if (label != currentLabel) {
                rows.add(RecentlyViewedRow.Header(label))
                currentLabel = label
            }
            rows.add(RecentlyViewedRow.Item(entry))
            entries.add(entry)
            entriesById[restaurant.id] = entry
        }
        return RecentlyViewedUi(entries, rows, entriesById)
    }

    suspend fun <T> forEachConcurrently(
        items: List<T>,
        maxParallel: Int = PREFETCH_PARALLELISM,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend (T) -> Unit
    ) = withContext(dispatcher) {
        val permits = Semaphore(maxParallel.coerceAtLeast(1))
        coroutineScope {
            items.map { item -> async { permits.withPermit { block(item) } } }.awaitAll()
        }
    }

    private fun isSortedDescending(views: List<RecentView>): Boolean {
        for (i in 1 until views.size) {
            if (views[i - 1].lastViewedAt < views[i].lastViewedAt) return false
        }
        return true
    }

    private fun startOfDay(calendar: Calendar, millis: Long): Long {
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
