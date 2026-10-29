package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.parseRestaurants
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantRepositoryTest {

    private val payload = JSONObject(
        """
        {"restaurants":[
          {"id":"1","name":"Starbucks","location":"Plazoleta","category":"Café","image_key":"starbucks",
           "image_url":null,"rating":4.7,"review_count":10,"price":"$","wait_time":"2 min",
           "tags":["Coffee"],"hours":"6:30 AM – 11:00 PM","description":"d","map_x":55.0,"map_y":25.0,
           "latitude":4.6,"longitude":-74.0,
           "menu":[{"category":"Drinks","items":[{"name":"Latte","price":"$1","description":"x"}]}]},
          {"id":"4","name":"Cosechas","location":"ML","category":"Smoothies","image_key":null,
           "image_url":"https://img/x.jpg","rating":4.4,"review_count":2,"price":"$$","wait_time":"3 min",
           "tags":[],"hours":"6:30 AM – 8:00 PM","description":"d","map_x":60.0,"map_y":35.0,
           "latitude":4.6,"longitude":-74.0,"menu":[]}
        ]}
        """.trimIndent()
    )

    private class FakeApi(private val response: JSONObject?) : RemoteApi {
        var calls = 0
        override suspend fun get(path: String): JSONObject {
            calls++
            return response ?: throw IOException("offline")
        }
        override suspend fun post(path: String, body: Map<String, Any>) = JSONObject()
        override suspend fun put(path: String, body: Map<String, Any>?) = JSONObject()
        override suspend fun delete(path: String) = JSONObject()
    }

    private val imageFor: (String) -> Int? = { if (it == "starbucks") 42 else null }
    private val clock = FakeClock()
    private val policy = CachePolicy(clock)

    private fun repository(api: RemoteApi, cache: FakeDocumentCache) =
        RestaurantRepository(api, cache, imageFor, policy)

    @Test
    fun parsesFieldsMenuAndImages() {
        val parsed = parseRestaurants(payload, imageFor)
        assertEquals(listOf("1", "4"), parsed.map { it.id })
        assertEquals(42, parsed[0].image)
        assertEquals("https://img/x.jpg", parsed[1].image)
        assertEquals("Latte", parsed[0].menu[0].items[0].name)
        assertEquals(10, parsed[0].reviews)
        assertEquals(55f, parsed[0].mapX)
    }

    @Test
    fun fetchStoresCacheWithTimestampAndCachedReadsItBack() = runTest {
        val cache = FakeDocumentCache()
        val repository = repository(FakeApi(payload), cache)
        assertTrue(repository.cached().isEmpty())
        assertEquals(2, repository.fetch().size)
        assertEquals(clock.now, cache.fetchedAt(CacheKeys.RESTAURANTS))
        assertEquals(listOf("1", "4"), repository.cached().map { it.id })
    }

    @Test
    fun failedFetchKeepsCachedData() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), clock.now) }
        val repository = repository(FakeApi(null), cache)
        assertTrue(runCatching { repository.fetch() }.isFailure)
        assertEquals(2, repository.cached().size)
    }

    @Test
    fun corruptCacheYieldsEmptyListAndIsDiscarded() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, "not json", clock.now) }
        assertTrue(repository(FakeApi(null), cache).cached().isEmpty())
        assertFalse(cache.has(CacheKeys.RESTAURANTS))
    }

    @Test
    fun payloadWithMissingFieldsIsDiscarded() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, """{"restaurants":[{"id":"1"}]}""", clock.now) }
        assertTrue(repository(FakeApi(null), cache).cached().isEmpty())
        assertFalse(cache.has(CacheKeys.RESTAURANTS))
    }

    @Test
    fun revalidateSkipsTheNetworkWhileFresh() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), clock.now) }
        val api = FakeApi(payload)
        clock.now += CacheTtl.RESTAURANTS - 1
        assertNull(repository(api, cache).revalidate())
        assertEquals(0, api.calls)
    }

    @Test
    fun revalidateFetchesOnceTheTtlExpires() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), clock.now) }
        val api = FakeApi(payload)
        clock.now += CacheTtl.RESTAURANTS
        assertEquals(2, repository(api, cache).revalidate()?.size)
        assertEquals(1, api.calls)
        assertEquals(clock.now, cache.fetchedAt(CacheKeys.RESTAURANTS))
    }

    @Test
    fun revalidateForceIgnoresFreshness() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), clock.now) }
        val api = FakeApi(payload)
        assertNotNull(repository(api, cache).revalidate(force = true))
        assertEquals(1, api.calls)
    }

    @Test
    fun staleCacheIsStillServedWhenRevalidationFails() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), clock.now) }
        clock.now += CacheTtl.RESTAURANTS * 2
        val repository = repository(FakeApi(null), cache)
        assertTrue(repository.isStale())
        assertTrue(runCatching { repository.revalidate() }.isFailure)
        assertEquals(2, repository.cached().size)
    }

    @Test
    fun migratedCacheWithZeroTimestampIsStale() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.RESTAURANTS, payload.toString(), 0L) }
        assertTrue(repository(FakeApi(payload), cache).isStale())
    }
}
