package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object RankingEngine {

    private const val PRIOR_WEIGHT = 3.0
    private const val CROWDING_SCALE_TOP = 6.0
    private const val NEUTRAL_CROWDING = 3.0

    private class Prepared(
        val inputs: RankingInputs,
        val window: RankingWindow,
        val reviewsByRestaurant: Map<String, List<Review>>,
        val allReviewsByRestaurant: Map<String, List<Review>>
    )

    private class Sample(val restaurant: Restaurant, val average: Double, val count: Int, val fallback: Boolean = false)

    suspend fun computeAll(inputs: RankingInputs, window: RankingWindow): Map<RankingCriterion, List<RankedRestaurant>> {
        val prepared = withContext(Dispatchers.Default) { prepare(inputs, window) }
        return coroutineScope {
            RankingCriterion.entries
                .map { criterion -> criterion to async(Dispatchers.Default) { rank(prepared, criterion) } }
                .map { (criterion, deferred) -> criterion to deferred.await() }
                .toMap()
        }
    }

    suspend fun compute(inputs: RankingInputs, criterion: RankingCriterion, window: RankingWindow): List<RankedRestaurant> =
        withContext(Dispatchers.Default) { rank(prepare(inputs, window), criterion) }

    private suspend fun prepare(inputs: RankingInputs, window: RankingWindow): Prepared {
        currentCoroutineContext().ensureActive()
        val grouped = inputs.reviews
            .filter { window == RankingWindow.ALL_TIME || it.date >= inputs.windowCutoff }
            .groupBy { it.restaurantId }
        return Prepared(inputs, window, grouped, inputs.reviews.groupBy { it.restaurantId })
    }

    private suspend fun rank(prepared: Prepared, criterion: RankingCriterion): List<RankedRestaurant> {
        val context = currentCoroutineContext()
        val samples = prepared.inputs.restaurants.mapNotNull { restaurant ->
            context.ensureActive()
            sampleFor(
                restaurant,
                prepared.reviewsByRestaurant[restaurant.id].orEmpty(),
                prepared.allReviewsByRestaurant[restaurant.id].orEmpty(),
                prepared.inputs.crowding[restaurant.id].orEmpty(),
                criterion,
                prepared.window
            )
        }
        if (samples.isEmpty()) return emptyList()

        val globalMean = samples.sumOf { it.average * it.count } / samples.sumOf { it.count }.coerceAtLeast(1)

        context.ensureActive()
        return samples
            .map { it to smooth(it.average, it.count, globalMean) }
            .sortedWith(
                compareBy<Pair<Sample, Double>> { it.first.fallback }
                    .thenByDescending { it.second }
                    .thenByDescending { it.first.count }
                    .thenBy { it.first.restaurant.name }
            )
            .mapIndexed { index, (sample, score) ->
                RankedRestaurant(
                    position = index + 1,
                    restaurant = sample.restaurant,
                    score = score,
                    reviewCount = sample.count,
                    movement = null,
                    isFallback = sample.fallback
                )
            }
    }

    private fun sampleFor(
        restaurant: Restaurant,
        reviews: List<Review>,
        allReviews: List<Review>,
        crowdingReports: List<Int>,
        criterion: RankingCriterion,
        window: RankingWindow
    ): Sample? = when (criterion) {
        RankingCriterion.LESS_CROWDED -> {
            val average = if (crowdingReports.isEmpty()) NEUTRAL_CROWDING else CROWDING_SCALE_TOP - crowdingReports.average()
            Sample(restaurant, average, crowdingReports.size)
        }
        else -> {
            val values = reviews.map { valueOf(it, criterion) }
            val outsideWindow = window == RankingWindow.MONTH
            when {
                values.isNotEmpty() -> Sample(restaurant, values.average(), values.size)
                allReviews.isNotEmpty() -> {
                    val older = allReviews.map { valueOf(it, criterion) }
                    Sample(restaurant, older.average(), older.size, fallback = outsideWindow)
                }
                criterion == RankingCriterion.OVERALL && restaurant.reviews > 0 ->
                    Sample(restaurant, restaurant.rating, restaurant.reviews, fallback = outsideWindow)
                else -> null
            }
        }
    }

    private fun valueOf(review: Review, criterion: RankingCriterion): Double = when (criterion) {
        RankingCriterion.OVERALL -> review.ratings.overall
        RankingCriterion.TASTE -> review.ratings.taste.toDouble()
        RankingCriterion.SERVICE -> review.ratings.attention.toDouble()
        RankingCriterion.VALUE -> review.ratings.price.toDouble()
        RankingCriterion.LESS_CROWDED -> 0.0
    }

    private fun smooth(average: Double, count: Int, globalMean: Double): Double =
        (count / (count + PRIOR_WEIGHT)) * average + (PRIOR_WEIGHT / (count + PRIOR_WEIGHT)) * globalMean
}
