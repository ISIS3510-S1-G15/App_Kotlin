package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.restaurants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RankingCacheTest {

    private fun result(criterion: RankingCriterion, window: RankingWindow = RankingWindow.ALL_TIME) =
        RankingResult(criterion, window, emptyList(), computedAt = 1L, fromSnapshot = false)

    private fun review(id: String, taste: Int) = Review(
        id = id,
        restaurantId = "1",
        author = "a",
        date = "2026-11-20",
        ratings = ReviewRatings(taste, 4, 4, 4),
        dietaryOptions = emptyList(),
        comment = ""
    )

    private fun inputs(reviews: List<Review>, crowding: Map<String, List<Int>> = emptyMap()) =
        RankingInputs(restaurants, reviews, crowding, windowCutoff = "2026-11-16")

    @Test
    fun hitWhenFingerprintMatches() {
        val cache = RankingCache()
        val stored = result(RankingCriterion.TASTE)
        cache.put(stored, 42L)
        assertSame(stored, cache.get(RankingCriterion.TASTE, RankingWindow.ALL_TIME, 42L))
        assertEquals(1, cache.hits)
        assertEquals(0, cache.misses)
    }

    @Test
    fun missWhenFingerprintChanges() {
        val cache = RankingCache()
        cache.put(result(RankingCriterion.TASTE), 42L)
        assertNull(cache.get(RankingCriterion.TASTE, RankingWindow.ALL_TIME, 43L))
        assertEquals(1, cache.misses)
    }

    @Test
    fun missForOtherCriterionOrWindow() {
        val cache = RankingCache()
        cache.put(result(RankingCriterion.TASTE), 42L)
        assertNull(cache.get(RankingCriterion.VALUE, RankingWindow.ALL_TIME, 42L))
        assertNull(cache.get(RankingCriterion.TASTE, RankingWindow.MONTH, 42L))
    }

    @Test
    fun putAllStoresEveryCriterion() {
        val cache = RankingCache()
        cache.putAll(RankingCriterion.entries.map { result(it) }, 7L)
        RankingCriterion.entries.forEach {
            assertNotNull(cache.get(it, RankingWindow.ALL_TIME, 7L))
        }
    }

    @Test
    fun leastRecentlyUsedEntryIsEvicted() {
        val cache = RankingCache(maxEntries = 2)
        cache.put(result(RankingCriterion.TASTE), 1L)
        cache.put(result(RankingCriterion.VALUE), 1L)
        cache.get(RankingCriterion.TASTE, RankingWindow.ALL_TIME, 1L)
        cache.put(result(RankingCriterion.SERVICE), 1L)
        assertNotNull(cache.get(RankingCriterion.TASTE, RankingWindow.ALL_TIME, 1L))
        assertNull(cache.get(RankingCriterion.VALUE, RankingWindow.ALL_TIME, 1L))
    }

    @Test
    fun clearEmptiesTheCache() {
        val cache = RankingCache()
        cache.put(result(RankingCriterion.TASTE), 1L)
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun trimKeepsOnlyTheRequestedFraction() {
        val cache = RankingCache()
        cache.putAll(RankingCriterion.entries.map { result(it) }, 1L)
        cache.trim(0.4f)
        assertEquals(2, cache.size())
    }

    @Test
    fun fingerprintIsStableForSameInputs() {
        val data = inputs(listOf(review("r1", 5)))
        assertEquals(
            RankingFingerprint.of(data, RankingWindow.ALL_TIME),
            RankingFingerprint.of(inputs(listOf(review("r1", 5))), RankingWindow.ALL_TIME)
        )
    }

    @Test
    fun fingerprintChangesWhenReviewsChange() {
        val before = RankingFingerprint.of(inputs(listOf(review("r1", 5))), RankingWindow.ALL_TIME)
        val added = RankingFingerprint.of(inputs(listOf(review("r1", 5), review("r2", 3))), RankingWindow.ALL_TIME)
        val edited = RankingFingerprint.of(inputs(listOf(review("r1", 2))), RankingWindow.ALL_TIME)
        assertNotEquals(before, added)
        assertNotEquals(before, edited)
    }

    @Test
    fun fingerprintChangesWhenCrowdingChanges() {
        val before = RankingFingerprint.of(inputs(emptyList(), mapOf("1" to listOf(1))), RankingWindow.ALL_TIME)
        val after = RankingFingerprint.of(inputs(emptyList(), mapOf("1" to listOf(1, 5))), RankingWindow.ALL_TIME)
        assertNotEquals(before, after)
    }

    @Test
    fun fingerprintDiffersPerWindow() {
        val data = inputs(listOf(review("r1", 5)))
        assertNotEquals(
            RankingFingerprint.of(data, RankingWindow.MONTH),
            RankingFingerprint.of(data, RankingWindow.ALL_TIME)
        )
    }
}
