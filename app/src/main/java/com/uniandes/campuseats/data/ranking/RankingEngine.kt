package com.uniandes.campuseats.data.ranking

import androidx.collection.ArrayMap
import com.uniandes.campuseats.data.Restaurant
import java.util.Arrays
import java.util.EnumMap
import kotlinx.coroutines.Deferred
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
    private const val CANCELLATION_CHECK_MASK = 255
    private const val DIMENSIONS = 4
    private const val OVERALL = 0
    private const val TASTE = 1
    private const val SERVICE = 2
    private const val VALUE = 3

    private class Accumulator {
        val windowSum = DoubleArray(DIMENSIONS)
        var windowCount = 0
        val allSum = DoubleArray(DIMENSIONS)
        var allCount = 0
    }

    private class Prepared(
        val inputs: RankingInputs,
        val window: RankingWindow,
        val accumulators: ArrayMap<String, Accumulator>
    )

    private class Candidate(
        val restaurant: Restaurant,
        val average: Double,
        val count: Int,
        val fallback: Boolean
    ) {
        var score = 0.0
    }

    private val CANDIDATE_ORDER = Comparator<Candidate> { first, second ->
        when {
            first.fallback != second.fallback -> if (first.fallback) 1 else -1
            else -> {
                val byScore = java.lang.Double.compare(second.score, first.score)
                if (byScore != 0) byScore else {
                    val byCount = Integer.compare(second.count, first.count)
                    if (byCount != 0) byCount else first.restaurant.name.compareTo(second.restaurant.name)
                }
            }
        }
    }

    suspend fun computeAll(inputs: RankingInputs, window: RankingWindow): Map<RankingCriterion, List<RankedRestaurant>> {
        val prepared = withContext(Dispatchers.Default) { prepare(inputs, window) }
        return coroutineScope {
            val criteria = RankingCriterion.entries
            val jobs = ArrayList<Deferred<List<RankedRestaurant>>>(criteria.size)
            for (index in criteria.indices) {
                val criterion = criteria[index]
                jobs.add(async(Dispatchers.Default) { rank(prepared, criterion) })
            }
            val results = EnumMap<RankingCriterion, List<RankedRestaurant>>(RankingCriterion::class.java)
            for (index in criteria.indices) results[criteria[index]] = jobs[index].await()
            results
        }
    }

    suspend fun compute(inputs: RankingInputs, criterion: RankingCriterion, window: RankingWindow): List<RankedRestaurant> =
        withContext(Dispatchers.Default) { rank(prepare(inputs, window), criterion) }

    private suspend fun prepare(inputs: RankingInputs, window: RankingWindow): Prepared {
        val context = currentCoroutineContext()
        val accumulators = ArrayMap<String, Accumulator>(inputs.restaurants.size)
        val reviews = inputs.reviews
        val everything = window == RankingWindow.ALL_TIME
        val cutoff = inputs.windowCutoff
        for (index in reviews.indices) {
            if (index and CANCELLATION_CHECK_MASK == 0) context.ensureActive()
            val review = reviews[index]
            val accumulator = accumulators[review.restaurantId] ?: Accumulator().also { accumulators[review.restaurantId] = it }
            val ratings = review.ratings
            val overall = (ratings.taste + ratings.attention + ratings.price + ratings.options) / 4.0
            accumulator.allSum[OVERALL] += overall
            accumulator.allSum[TASTE] += ratings.taste
            accumulator.allSum[SERVICE] += ratings.attention
            accumulator.allSum[VALUE] += ratings.price
            accumulator.allCount++
            if (everything || review.date >= cutoff) {
                accumulator.windowSum[OVERALL] += overall
                accumulator.windowSum[TASTE] += ratings.taste
                accumulator.windowSum[SERVICE] += ratings.attention
                accumulator.windowSum[VALUE] += ratings.price
                accumulator.windowCount++
            }
        }
        return Prepared(inputs, window, accumulators)
    }

    private suspend fun rank(prepared: Prepared, criterion: RankingCriterion): List<RankedRestaurant> {
        val context = currentCoroutineContext()
        val restaurants = prepared.inputs.restaurants
        val candidates = arrayOfNulls<Candidate>(restaurants.size)
        var total = 0
        var weightedSum = 0.0
        var totalCount = 0
        for (index in restaurants.indices) {
            context.ensureActive()
            val candidate = candidateFor(restaurants[index], prepared, criterion) ?: continue
            candidates[total++] = candidate
            weightedSum += candidate.average * candidate.count
            totalCount += candidate.count
        }
        if (total == 0) return emptyList()

        val globalMean = weightedSum / totalCount.coerceAtLeast(1)
        for (index in 0 until total) {
            val candidate = candidates[index]!!
            candidate.score = smooth(candidate.average, candidate.count, globalMean)
        }

        context.ensureActive()
        @Suppress("UNCHECKED_CAST")
        Arrays.sort(candidates as Array<Candidate>, 0, total, CANDIDATE_ORDER)

        val ranked = ArrayList<RankedRestaurant>(total)
        for (index in 0 until total) {
            val candidate = candidates[index]!!
            ranked.add(
                RankedRestaurant(
                    position = index + 1,
                    restaurant = candidate.restaurant,
                    score = candidate.score,
                    reviewCount = candidate.count,
                    movement = null,
                    isFallback = candidate.fallback
                )
            )
        }
        return ranked
    }

    private fun candidateFor(restaurant: Restaurant, prepared: Prepared, criterion: RankingCriterion): Candidate? {
        if (criterion == RankingCriterion.LESS_CROWDED) {
            val reports = prepared.inputs.crowding[restaurant.id]
            val count = reports?.size ?: 0
            var sum = 0
            for (index in 0 until count) sum += reports!![index]
            val average = if (count == 0) NEUTRAL_CROWDING else CROWDING_SCALE_TOP - sum.toDouble() / count
            return Candidate(restaurant, average, count, fallback = false)
        }

        val dimension = dimensionOf(criterion)
        val outsideWindow = prepared.window == RankingWindow.MONTH
        val accumulator = prepared.accumulators[restaurant.id]
        return when {
            accumulator != null && accumulator.windowCount > 0 ->
                Candidate(restaurant, accumulator.windowSum[dimension] / accumulator.windowCount, accumulator.windowCount, false)
            accumulator != null && accumulator.allCount > 0 ->
                Candidate(restaurant, accumulator.allSum[dimension] / accumulator.allCount, accumulator.allCount, outsideWindow)
            criterion == RankingCriterion.OVERALL && restaurant.reviews > 0 ->
                Candidate(restaurant, restaurant.rating, restaurant.reviews, outsideWindow)
            else -> null
        }
    }

    private fun dimensionOf(criterion: RankingCriterion): Int = when (criterion) {
        RankingCriterion.OVERALL -> OVERALL
        RankingCriterion.TASTE -> TASTE
        RankingCriterion.SERVICE -> SERVICE
        RankingCriterion.VALUE -> VALUE
        RankingCriterion.LESS_CROWDED -> OVERALL
    }

    private fun smooth(average: Double, count: Int, globalMean: Double): Double =
        (count / (count + PRIOR_WEIGHT)) * average + (PRIOR_WEIGHT / (count + PRIOR_WEIGHT)) * globalMean
}
