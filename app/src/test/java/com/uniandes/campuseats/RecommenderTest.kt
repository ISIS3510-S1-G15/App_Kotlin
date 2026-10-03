package com.uniandes.campuseats

import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.data.emptyProfile
import com.uniandes.campuseats.data.recommend
import com.uniandes.campuseats.data.restaurants
import com.uniandes.campuseats.data.seedCrowding
import com.uniandes.campuseats.data.seedReviews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderTest {

    private fun minutes(hour: Int, minute: Int = 0) = hour * 60 + minute

    private val asianVegan: Profile = emptyProfile.copy(
        name = "Test",
        budget = "$15.000 – $25.000",
        dietaryRestrictions = listOf("Vegan"),
        cuisinePreferences = listOf("Asian"),
        topPriorities = listOf("Price")
    )

    private fun pick(
        profile: Profile,
        now: Int,
        crowding: Map<String, List<Int>> = seedCrowding,
        distances: Map<String, Float> = emptyMap()
    ) = recommend(restaurants, profile, seedReviews, crowding, distances, now)

    @Test
    fun recommends_the_cuisine_the_user_likes_and_explains_why() {
        val result = pick(asianVegan, minutes(13))
        assertEquals("Kai Sushi", result.first().restaurant.name)
        assertTrue(result.first().reasons.any { it.contains("Asian") })
    }

    @Test
    fun only_open_places_are_recommended() {
        // A las 9:30 PM solo Starbucks sigue abierto
        val result = pick(asianVegan, minutes(21, 30))
        assertEquals(listOf("Starbucks"), result.map { it.restaurant.name })
    }

    @Test
    fun when_everything_is_closed_it_still_recommends_and_says_so() {
        val result = pick(asianVegan, minutes(3))
        assertEquals(restaurants.size, result.size)
        assertTrue(result.all { "Closed right now" in it.reasons })
    }

    @Test
    fun proximity_priority_favors_the_closest_place() {
        val distances = mapOf("1" to 4000f, "2" to 4000f, "3" to 40f, "4" to 4000f)
        val result = pick(emptyProfile.copy(topPriorities = listOf("Proximity")), minutes(13), distances = distances)
        assertEquals("La Puerta", result.first().restaurant.name)
    }

    @Test
    fun foods_to_avoid_lower_the_score() {
        val base = pick(emptyProfile, minutes(13)).first { it.restaurant.name == "La Puerta" }.score
        val avoiding = pick(emptyProfile.copy(foodsToAvoid = listOf("mushroom")), minutes(13))
            .first { it.restaurant.name == "La Puerta" }.score
        assertTrue(avoiding < base)
    }

    @Test
    fun crowded_place_scores_lower_when_user_prioritizes_speed() {
        val base = pick(emptyProfile, minutes(13)).first { it.restaurant.name == "Cosechas" }.score
        val crowded = seedCrowding + ("4" to listOf(5, 5, 5))
        val speed = pick(emptyProfile.copy(topPriorities = listOf("Speed")), minutes(13), crowding = crowded)
            .first { it.restaurant.name == "Cosechas" }.score
        assertTrue(speed < base)
    }
}
