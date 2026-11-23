package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.applyRatingSummaries
import com.uniandes.campuseats.net.attemptOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RankingRepository(
    private val restaurants: RestaurantRepository,
    private val reviews: ReviewRepository,
    private val occupancy: OccupancyRepository,
    private val localStore: LocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun load(criterion: RankingCriterion, window: RankingWindow, refresh: Boolean): RankingResult {
        if (refresh) refreshSources()
        val inputs = gatherInputs()
        val items = RankingEngine.compute(inputs, criterion, window)
        return RankingResult(criterion, window, items, clock(), fromSnapshot = false)
    }

    private suspend fun refreshSources() {
        attemptOrNull { restaurants.revalidate(force = true) }
        attemptOrNull { reviews.revalidateSummaries(force = true) }
        val ids = restaurants.cached().map { it.id }
        if (ids.isNotEmpty()) {
            val fetched = attemptOrNull { reviews.revalidateAll(ids, force = true) }.orEmpty()
            if (fetched.isNotEmpty()) {
                val current = attemptOrNull { localStore.loadReviews() }.orEmpty()
                val merged = ids.flatMap { id -> fetched[id] ?: current.filter { it.restaurantId == id } }
                attemptOrNull { localStore.saveReviews(merged) }
            }
        }
    }

    private suspend fun gatherInputs(): RankingInputs {
        val base = restaurants.cached()
        val summaries = reviews.cachedSummaries()
        val allReviews = attemptOrNull { localStore.loadReviews() }.orEmpty()
        val crowding = attemptOrNull { occupancy.revalidate() } ?: attemptOrNull { localStore.loadCrowding() }.orEmpty()
        return RankingInputs(
            restaurants = applyRatingSummaries(base, summaries),
            reviews = allReviews,
            crowding = crowding,
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

    private companion object {
        const val WEEK_DAYS = 7
    }
}
