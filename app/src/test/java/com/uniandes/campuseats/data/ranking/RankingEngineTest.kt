package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.restaurants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingEngineTest {

    private fun review(id: String, restaurantId: String, date: String, taste: Int) = Review(
        id = id,
        restaurantId = restaurantId,
        author = "a",
        date = date,
        ratings = ReviewRatings(taste, 4, 4, 4),
        dietaryOptions = emptyList(),
        comment = ""
    )

    private fun inputs(reviews: List<Review>, crowding: Map<String, List<Int>> = emptyMap()) =
        RankingInputs(restaurants, reviews, crowding, weekCutoff = "2026-11-16")

    @Test
    fun computeAllReturnsEveryCriterion() = runTest {
        val result = RankingEngine.computeAll(inputs(listOf(review("r1", "1", "2026-11-20", 5))), RankingWindow.ALL_TIME)
        assertEquals(RankingCriterion.entries.toSet(), result.keys)
    }

    @Test
    fun computeAllMatchesSingleCriterionComputation() = runTest {
        val reviews = listOf(
            review("r1", "1", "2026-11-20", 5), review("r2", "2", "2026-11-20", 3),
            review("r3", "3", "2026-11-20", 4)
        )
        val all = RankingEngine.computeAll(inputs(reviews), RankingWindow.ALL_TIME)
        RankingCriterion.entries.forEach { criterion ->
            val single = RankingEngine.compute(inputs(reviews), criterion, RankingWindow.ALL_TIME)
            assertEquals(single.map { it.restaurant.id }, all.getValue(criterion).map { it.restaurant.id })
        }
    }

    @Test
    fun higherAverageRanksFirst() = runTest {
        val reviews = listOf(
            review("r1", "1", "2026-11-20", 5), review("r2", "1", "2026-11-20", 5),
            review("r3", "2", "2026-11-20", 2), review("r4", "2", "2026-11-20", 2)
        )
        val ranking = RankingEngine.compute(inputs(reviews), RankingCriterion.TASTE, RankingWindow.ALL_TIME)
        assertEquals(listOf("1", "2"), ranking.map { it.restaurant.id })
        assertEquals(listOf(1, 2), ranking.map { it.position })
    }

    @Test
    fun smoothingPenalisesSingleReview() = runTest {
        val reviews = listOf(review("r1", "1", "2026-11-20", 5)) +
            (1..10).map { review("m$it", "2", "2026-11-20", 5) } +
            (1..10).map { review("n$it", "3", "2026-11-20", 3) }
        val ranking = RankingEngine.compute(inputs(reviews), RankingCriterion.TASTE, RankingWindow.ALL_TIME)
        assertEquals("2", ranking.first().restaurant.id)
    }

    @Test
    fun weekWindowIgnoresOldReviews() = runTest {
        val reviews = listOf(
            review("r1", "1", "2026-11-01", 5),
            review("r2", "2", "2026-11-20", 3)
        )
        val ranking = RankingEngine.compute(inputs(reviews), RankingCriterion.TASTE, RankingWindow.WEEK)
        assertEquals(listOf("2"), ranking.map { it.restaurant.id })
    }

    @Test
    fun lessCrowdedPrefersLowerLevels() = runTest {
        val crowding = mapOf("1" to listOf(5, 5), "2" to listOf(1, 1), "3" to listOf(3, 3))
        val ranking = RankingEngine.compute(inputs(emptyList(), crowding), RankingCriterion.LESS_CROWDED, RankingWindow.ALL_TIME)
        assertEquals("2", ranking.first().restaurant.id)
    }

    @Test
    fun cancelledComputationThrowsCancellation() {
        val reviews = (1..2000).map { review("r$it", "1", "2026-11-20", 5) }
        val cancelled = runBlocking {
            val job = async(Dispatchers.Default, CoroutineStart.LAZY) {
                RankingEngine.computeAll(inputs(reviews), RankingWindow.ALL_TIME)
            }
            job.cancel()
            try {
                job.await()
                false
            } catch (e: CancellationException) {
                true
            }
        }
        assertTrue(cancelled)
    }
}
