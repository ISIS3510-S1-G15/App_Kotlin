package com.uniandes.campuseats.ui

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

data class RecentlyViewedEntry(
    val restaurant: Restaurant,
    val view: RecentView,
    val isOpen: Boolean,
    val subtitle: String,
    val viewedLabel: String,
    val dayLabel: String
)

sealed interface RecentlyViewedRow {
    val key: String

    data class Header(val label: String) : RecentlyViewedRow {
        override val key: String get() = "header_$label"
    }

    data class Item(val entry: RecentlyViewedEntry) : RecentlyViewedRow {
        override val key: String get() = "item_${entry.restaurant.id}"
    }
}

data class RecentlyViewedUi(
    val entries: List<RecentlyViewedEntry>,
    val rows: List<RecentlyViewedRow>
) {
    companion object {
        val Empty = RecentlyViewedUi(emptyList(), emptyList())
    }
}

fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

object RecentlyViewedPresenter {

    const val PREFETCH_PARALLELISM = 3

    suspend fun build(
        views: List<RecentView>,
        restaurants: List<Restaurant>,
        now: Long = System.currentTimeMillis(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): RecentlyViewedUi = withContext(dispatcher) {
        compute(views, restaurants, now, currentMinutesOfDay())
    }

    suspend fun compute(
        views: List<RecentView>,
        restaurants: List<Restaurant>,
        now: Long,
        minutesOfDay: Int
    ): RecentlyViewedUi {
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH)
        val today = startOfDay(now)
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val byId = restaurants.associateBy { it.id }
        val context = currentCoroutineContext()
        val entries = views.sortedByDescending { it.lastViewedAt }.mapNotNull { view ->
            context.ensureActive()
            val restaurant = byId[view.restaurantId] ?: return@mapNotNull null
            val day = startOfDay(view.lastViewedAt)
            val label = when {
                day >= today -> "TODAY"
                day == yesterday -> "YESTERDAY"
                else -> dayFormat.format(Date(view.lastViewedAt)).uppercase(Locale.ENGLISH)
            }
            RecentlyViewedEntry(
                restaurant = restaurant,
                view = view,
                isOpen = isOpenAt(restaurant.hours, minutesOfDay),
                subtitle = "${restaurant.category} · ${restaurant.location}",
                viewedLabel = "Viewed ${view.viewCount} ${if (view.viewCount == 1) "time" else "times"} · " +
                    "last at ${clock.format(Date(view.lastViewedAt))}",
                dayLabel = label
            )
        }
        val rows = buildList {
            entries.groupBy { it.dayLabel }.forEach { (label, group) ->
                add(RecentlyViewedRow.Header(label))
                group.forEach { add(RecentlyViewedRow.Item(it)) }
            }
        }
        return RecentlyViewedUi(entries, rows)
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

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
