package com.uniandes.campuseats.data.ranking

import android.util.Log
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.applyRatingSummaries
import com.uniandes.campuseats.net.attemptOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RankingRepository(
    private val restaurants: RestaurantRepository,
    private val reviews: ReviewRepository,
    private val occupancy: OccupancyRepository,
    private val localStore: LocalStore,
    private val cache: RankingCache = RankingCache(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun load(criterion: RankingCriterion, window: RankingWindow, refresh: Boolean): RankingResult {
        val startedAt = System.nanoTime()
        if (refresh) refreshSources()
        val refreshedAt = System.nanoTime()
        val inputs = gatherInputs()
        val fingerprint = RankingFingerprint.of(inputs, window)
        val gatheredAt = System.nanoTime()

        cache.get(criterion, window, fingerprint)?.let { cached ->
            Log.d(
                TAG,
                "cache=HIT inputs=${millis(gatheredAt - refreshedAt)}ms hits=${cache.hits} misses=${cache.misses} entries=${cache.size()}"
            )
            return cached
        }

        val all = RankingEngine.computeAll(inputs, window)
        val computedAt = System.nanoTime()
        val now = clock()
        val results = all.map { (key, items) -> RankingResult(key, window, items, now, fromSnapshot = false) }
        cache.putAll(results, fingerprint)
        Log.d(
            TAG,
            "cache=MISS refresh=${millis(refreshedAt - startedAt)}ms inputs=${millis(gatheredAt - refreshedAt)}ms " +
                "compute=${millis(computedAt - gatheredAt)}ms hits=${cache.hits} misses=${cache.misses} " +
                "restaurants=${inputs.restaurants.size} reviews=${inputs.reviews.size}"
        )
        return results.first { it.criterion == criterion }
    }

    private suspend fun refreshSources() = coroutineScope {
        val restaurantsJob = async(Dispatchers.IO) { attemptOrNull { restaurants.revalidate(force = true) } }
        val summariesJob = async(Dispatchers.IO) { attemptOrNull { reviews.revalidateSummaries(force = true) } }
        val occupancyJob = async(Dispatchers.IO) { attemptOrNull { occupancy.revalidate(force = true) } }
        val fetchedRestaurants = restaurantsJob.await()
        summariesJob.await()
        occupancyJob.await()?.let { fetched -> attemptOrNull { localStore.saveCrowding(fetched) } }

        val ids = (fetchedRestaurants ?: restaurants.cached()).map { it.id }
        if (ids.isNotEmpty()) {
            val fetched = attemptOrNull { reviews.revalidateAll(ids, force = true) }.orEmpty()
            if (fetched.isNotEmpty()) {
                val current = attemptOrNull { localStore.loadReviews() }.orEmpty()
                val merged = ids.flatMap { id -> fetched[id] ?: current.filter { it.restaurantId == id } }
                attemptOrNull { localStore.saveReviews(merged) }
            }
        }
    }

    private suspend fun gatherInputs(): RankingInputs = coroutineScope {
        val base = async(Dispatchers.IO) { restaurants.cached() }
        val summaries = async(Dispatchers.IO) { reviews.cachedSummaries() }
        val allReviews = async(Dispatchers.IO) { attemptOrNull { localStore.loadReviews() }.orEmpty() }
        val crowding = async(Dispatchers.IO) { attemptOrNull { localStore.loadCrowding() }.orEmpty() }
        RankingInputs(
            restaurants = applyRatingSummaries(base.await(), summaries.await()),
            reviews = allReviews.await(),
            crowding = crowding.await(),
            weekCutoff = weekCutoff()
        )
    }

    private fun weekCutoff(): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = clock()
            add(Calendar.DAY_OF_YEAR, -WEEK_DAYS)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun millis(nanos: Long) = nanos / NANOS_PER_MILLI

    private companion object {
        const val TAG = "Ranking"
        const val WEEK_DAYS = 7
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
