package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.remote.OccupancyRepository
import com.uniandes.campuseats.data.remote.RatingSummary
import com.uniandes.campuseats.data.remote.ReviewRepository
import com.uniandes.campuseats.data.remote.appLevelFromRemote
import com.uniandes.campuseats.data.remote.applyRatingSummaries
import com.uniandes.campuseats.data.remote.remoteLevelFromApp
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRepositoriesDataTest {

    private class ScriptedApi(private val responses: Map<String, JSONObject>) : RemoteApi {
        val posts = mutableListOf<Pair<String, Map<String, Any>>>()
        override suspend fun get(path: String) = responses.getValue(path)
        override suspend fun post(path: String, body: Map<String, Any>): JSONObject {
            posts.add(path to body)
            return responses.getValue(path)
        }
        override suspend fun put(path: String, body: Map<String, Any>?) = JSONObject()
        override suspend fun delete(path: String) = JSONObject()
    }

    private fun reviewJson(id: Int = 7) = JSONObject(
        """{"id":$id,"restaurant_id":"1","author":"Ana","created_at":"2026-10-27T10:00:00+00:00",
        "taste":5,"attention":4,"price":3,"options":2,"dietary_options":["Vegan options"],"comment":"ok"}"""
    )

    @Test
    fun summariesSkipRestaurantsWithoutRating() = runTest {
        val items = JSONArray(
            """[{"restaurant_id":"1","rating":4.2,"review_count":3},
            {"restaurant_id":"2","rating":null,"review_count":0}]"""
        )
        val api = ScriptedApi(mapOf("/reviews/summary" to JSONObject().put("items", items)))
        val summaries = ReviewRepository(api).summaries()
        assertEquals(setOf("1"), summaries.keys)
        assertEquals(3, summaries.getValue("1").reviewCount)
    }

    @Test
    fun ratingSummariesOverrideOnlyMatchingRestaurants() {
        val merged = applyRatingSummaries(restaurants, mapOf("1" to RatingSummary("1", 3.5, 2)))
        assertEquals(3.5, merged.first { it.id == "1" }.rating, 0.0)
        assertEquals(2, merged.first { it.id == "1" }.reviews)
        assertEquals(restaurants.first { it.id == "2" }.rating, merged.first { it.id == "2" }.rating, 0.0)
    }

    @Test
    fun reviewsAreParsedWithDateAndAuthor() = runTest {
        val body = JSONObject().put("reviews", JSONArray().put(reviewJson()))
        val reviews = ReviewRepository(ScriptedApi(mapOf("/restaurants/1/reviews" to body))).forRestaurant("1")
        assertEquals("7", reviews[0].id)
        assertEquals("2026-10-27", reviews[0].date)
        assertEquals("Ana", reviews[0].author)
        assertEquals(listOf("Vegan options"), reviews[0].dietaryOptions)
    }

    @Test
    fun submitPostsRatingsToTheRestaurant() = runTest {
        val api = ScriptedApi(mapOf("/restaurants/1/reviews" to reviewJson()))
        val draft = Review("local", "1", "Ana", "2026-10-27", ReviewRatings(5, 4, 3, 2), listOf("Vegan options"), "ok")
        ReviewRepository(api).submit(draft)
        val (path, body) = api.posts.single()
        assertEquals("/restaurants/1/reviews", path)
        assertEquals(5, body["taste"])
        assertEquals("ok", body["comment"])
    }

    @Test
    fun occupancyLevelsConvertBetweenScales() {
        assertEquals(listOf(0, 0, 1, 2, 2), (1..5).map(::remoteLevelFromApp))
        assertEquals(listOf(1, 3, 5), (0..2).map(::appLevelFromRemote))
    }

    @Test
    fun occupancyFetchAndReportUseAppScale() = runTest {
        val all = JSONArray("""[{"restaurant_id":"1","level":1.0,"reports":[2,0]}]""")
        val reported = JSONObject("""{"restaurant_id":"1","level":2.0,"reports":[2]}""")
        val api = ScriptedApi(
            mapOf("/occupancy" to JSONObject().put("items", all), "/occupancy/reports" to reported)
        )
        val repository = OccupancyRepository(api)
        assertEquals(listOf(5, 1), repository.fetchAll().getValue("1"))
        assertEquals(listOf(5), repository.report("1", 4))
        assertEquals(2, api.posts.single().second["level"])
        assertTrue(api.posts.single().second["restaurant_id"] == "1")
    }
}
