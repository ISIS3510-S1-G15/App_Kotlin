package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewParallelismTest {

    private class SlowApi(private val failing: Set<String> = emptySet()) : RemoteApi {
        var active = 0
        var maxActive = 0
        var calls = 0

        override suspend fun get(path: String): JSONObject {
            calls++
            active++
            maxActive = maxOf(maxActive, active)
            try {
                delay(100)
                val id = path.removePrefix("/restaurants/").removeSuffix("/reviews")
                if (id in failing) throw IOException("boom")
                val review = JSONObject(
                    """{"id":$id,"restaurant_id":"$id","author":"Ana","created_at":"2026-10-27T10:00:00+00:00",
                    "taste":5,"attention":4,"price":3,"options":2,"dietary_options":[],"comment":"ok"}"""
                )
                return JSONObject().put("reviews", JSONArray().put(review))
            } finally {
                active--
            }
        }

        override suspend fun post(path: String, body: Map<String, Any>) = JSONObject()
        override suspend fun put(path: String, body: Map<String, Any>?) = JSONObject()
        override suspend fun delete(path: String) = JSONObject()
    }

    private val clock = FakeClock()
    private val ids = (1..10).map { it.toString() }

    private fun repository(api: RemoteApi) = ReviewRepository(api, FakeDocumentCache(), CachePolicy(clock))

    @Test
    fun requestsRunInParallelUpToTheLimit() = runTest {
        val api = SlowApi()
        val fetched = repository(api).revalidateAll(ids, parallelism = 4)
        assertEquals(ids.toSet(), fetched.keys)
        assertEquals(4, api.maxActive)
    }

    @Test
    fun aFailingRestaurantIsSkippedWithoutAffectingTheOthers() = runTest {
        val api = SlowApi(failing = setOf("3"))
        val fetched = repository(api).revalidateAll(ids)
        assertEquals(ids.toSet() - "3", fetched.keys)
        assertEquals("5", fetched.getValue("5").single().id)
    }

    @Test
    fun freshRestaurantsAreNotRequestedAgain() = runTest {
        val api = SlowApi()
        val repository = repository(api)
        repository.revalidateAll(ids)
        assertEquals(10, api.calls)
        assertTrue(repository.revalidateAll(ids).isEmpty())
        assertEquals(10, api.calls)
        assertEquals(10, repository.revalidateAll(ids, force = true).size)
        assertEquals(20, api.calls)
    }
}
