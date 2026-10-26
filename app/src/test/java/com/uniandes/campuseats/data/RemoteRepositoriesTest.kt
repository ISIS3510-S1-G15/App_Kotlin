package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.remote.ProfileRepository
import com.uniandes.campuseats.data.remote.SavedRepository
import com.uniandes.campuseats.data.remote.profileFromRemote
import com.uniandes.campuseats.data.remote.toRemoteBody
import com.uniandes.campuseats.net.FakeRemoteApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteRepositoriesTest {

    private val profile = Profile(
        name = "Ana",
        frequency = "Daily",
        budget = "Under 8.000",
        dietaryRestrictions = listOf("Vegan"),
        cuisinePreferences = listOf("Asian", "Italian"),
        usualMealTimes = listOf("Lunch"),
        topPriorities = listOf("Price", "Speed", "Taste", "Variety"),
        foodsToAvoid = listOf("Spicy", "Olives")
    )

    @Test
    fun profileBodyUsesBackendFieldNames() {
        val body = profile.toRemoteBody()
        assertEquals("Daily", body["meal_frequency"])
        assertEquals(listOf("Asian", "Italian"), body["cuisines"])
        assertEquals(listOf("Lunch"), body["meal_times"])
        assertEquals(listOf("Price", "Speed", "Taste"), body["priorities"])
        assertEquals("Spicy, Olives", body["disliked_foods"])
    }

    @Test
    fun profileRoundTripsThroughBackendFormat() {
        val json = JSONObject(profile.copy(topPriorities = profile.topPriorities.take(3)).toRemoteBody())
        assertEquals(profile.copy(topPriorities = listOf("Price", "Speed", "Taste")), profileFromRemote(json))
    }

    @Test
    fun fetchReturnsNullWhenSurveyWasNeverCompleted() = runTest {
        val api = FakeRemoteApi().apply { respond("/me/profile", """{"name":"","completed":false}""") }
        assertNull(ProfileRepository(api).fetch())
    }

    @Test
    fun fetchReturnsProfileWhenCompleted() = runTest {
        val api = FakeRemoteApi().apply {
            respond(
                "/me/profile",
                """{"name":"Ana","cuisines":["Asian"],"dietary_restrictions":[],"meal_frequency":"Daily",
                    "meal_times":["Lunch"],"budget":"","priorities":["Price"],"disliked_foods":"","completed":true}"""
            )
        }
        val fetched = ProfileRepository(api).fetch()
        assertEquals("Ana", fetched?.name)
        assertEquals(listOf("Asian"), fetched?.cuisinePreferences)
        assertEquals(emptyList<String>(), fetched?.foodsToAvoid)
    }

    @Test
    fun saveSendsProfileWithPut() = runTest {
        val api = FakeRemoteApi()
        ProfileRepository(api).save(profile)
        val call = api.calls.single()
        assertEquals("PUT", call.first)
        assertEquals("/me/profile", call.second)
    }

    @Test
    fun savedRepositoryParsesIdsAndUsesRestVerbs() = runTest {
        val api = FakeRemoteApi().apply {
            respond("/me/saved", """{"restaurant_ids":["a","b"]}""")
            respond("/me/saved/c", """{"restaurant_ids":["a","b","c"]}""")
        }
        val repository = SavedRepository(api)
        assertEquals(setOf("a", "b"), repository.fetch())
        assertEquals(setOf("a", "b", "c"), repository.add("c"))
        repository.remove("c")
        assertEquals(listOf("GET", "PUT", "DELETE"), api.calls.map { it.first })
    }
}
