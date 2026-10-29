package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {

    private val clock = FakeClock(10_000L)
    private val policy = CachePolicy(clock)

    @Test
    fun missingOrZeroTimestampIsNeverFresh() {
        assertFalse(policy.isFresh(null, 1_000L))
        assertFalse(policy.isFresh(0L, 1_000L))
    }

    @Test
    fun freshOnlyInsideTheTtlWindow() {
        assertTrue(policy.isFresh(9_500L, 1_000L))
        clock.now = 10_499L
        assertTrue(policy.isFresh(9_500L, 1_000L))
        clock.now = 10_500L
        assertFalse(policy.isFresh(9_500L, 1_000L))
    }

    @Test
    fun timestampInTheFutureIsTreatedAsStale() {
        assertFalse(policy.isFresh(20_000L, 1_000L))
    }

    @Test
    fun markFetchedStampsTheCurrentTime() = runTest {
        val cache = FakeDocumentCache()
        policy.markFetched(cache, "k")
        assertEquals(10_000L, cache.fetchedAt("k"))
        assertTrue(policy.isFreshOrFalse(cache, "k", CacheTtl.OCCUPANCY))
    }

    @Test
    fun reviewKeysAreScopedPerRestaurantAndVersioned() {
        assertEquals("reviews.v1.7", CacheKeys.reviews("7"))
        assertTrue(CacheKeys.RESTAURANTS.endsWith("v${CacheKeys.SCHEMA_VERSION}"))
    }
}
