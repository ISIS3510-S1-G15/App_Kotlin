package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review

object RankingEngine {

    private const val PRIOR_WEIGHT = 3.0
    private const val CROWDING_SCALE_TOP = 6.0
    private const val NEUTRAL_CROWDING = 3.0

    fun compute(inputs: RankingInputs, criterion: RankingCriterion, window: RankingWindow): List<RankedRestaurant> {
        val reviewsByRestaurant = inputs.reviews
            .filter { window == RankingWindow.ALL_TIME || it.date >= inputs.weekCutoff }
            .groupBy { it.restaurantId }

        val samples = inputs.restaurants.mapNotNull { restaurant ->
            sampleFor(restaurant, reviewsByRestaurant[restaurant.id].orEmpty(), inputs.crowding[restaurant.id].orEmpty(), criterion, window)
        }
        if (samples.isEmpty()) return emptyList()

        val globalMean = samples.sumOf { it.average * it.count } / samples.sumOf { it.count }.coerceAtLeast(1)

        return samples
            .map { it to smooth(it.average, it.count, globalMean) }
            .sortedWith(
                compareByDescending<Pair<Sample, Double>> { it.second }
                    .thenByDescending { it.first.count }
                    .thenBy { it.first.restaurant.name }
            )
            .mapIndexed { index, (sample, score) ->
                RankedRestaurant(
                    position = index + 1,
                    restaurant = sample.restaurant,
                    score = score,
                    reviewCount = sample.count,
                    movement = null
                )
            }
    }

    private class Sample(val restaurant: Restaurant, val average: Double, val count: Int)

    private fun sampleFor(
        restaurant: Restaurant,
        reviews: List<Review>,
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
            when {
                values.isNotEmpty() -> Sample(restaurant, values.average(), values.size)
                criterion == RankingCriterion.OVERALL && window == RankingWindow.ALL_TIME && restaurant.reviews > 0 ->
                    Sample(restaurant, restaurant.rating, restaurant.reviews)
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
