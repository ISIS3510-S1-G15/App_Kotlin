package com.uniandes.campuseats

import com.uniandes.campuseats.data.MealMoment
import com.uniandes.campuseats.data.closestOpenSpot
import com.uniandes.campuseats.data.greetingFor
import com.uniandes.campuseats.data.isOnCampus
import com.uniandes.campuseats.data.mealSlotFor
import com.uniandes.campuseats.data.restaurants
import com.uniandes.campuseats.data.walkingMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealContextTest {

    @Test
    fun mealSlotFollowsTheHourOfTheDay() {
        assertEquals("Breakfast", mealSlotFor(7))
        assertEquals("Mid-morning snack", mealSlotFor(10))
        assertEquals("Lunch", mealSlotFor(12))
        assertEquals("Afternoon snack", mealSlotFor(16))
        assertEquals("Dinner", mealSlotFor(20))
        assertEquals("Late night", mealSlotFor(23))
        assertEquals("Late night", mealSlotFor(3))
    }

    @Test
    fun greetingFollowsTheHourOfTheDay() {
        assertEquals("Good morning", greetingFor(8))
        assertEquals("Good afternoon", greetingFor(13))
        assertEquals("Good evening", greetingFor(22))
        assertEquals("Good evening", greetingFor(2))
    }

    @Test
    fun noMealMomentLateAtNight() {
        assertNull(MealMoment.at(23))
        assertNull(MealMoment.at(3))
        assertEquals(MealMoment.Lunch, MealMoment.at(12))
    }

    @Test
    fun walkingMinutesRoundUpAndNeverDropBelowOne() {
        assertEquals(1, walkingMinutes(10f))
        assertEquals(1, walkingMinutes(80f))
        assertEquals(2, walkingMinutes(81f))
        assertEquals(5, walkingMinutes(350f))
    }

    @Test
    fun campusRadiusIs600Meters() {
        assertTrue(isOnCampus(600f))
        assertFalse(isOnCampus(601f))
    }

    @Test
    fun closestOpenSpotIgnoresClosedPlacesAndPrefersTheMealThatFits() {
        val lunch = restaurants.first { MealMoment.Lunch.fits(it.category) }
        val other = restaurants.first { it.id != lunch.id && !MealMoment.Lunch.fits(it.category) }
        val distances = mapOf(other.id to 40f, lunch.id to 200f)
        val suggestion = closestOpenSpot(listOf(other, lunch), distances, hour = 12, minutesOfDay = 12 * 60)
        assertEquals(lunch.id, suggestion?.restaurant?.id)
        assertEquals(MealMoment.Lunch, suggestion?.meal)
    }

    @Test
    fun closestOpenSpotFallsBackToTheNearestOpenOneWhenNoMealFits() {
        val spot = restaurants.first()
        val suggestion = closestOpenSpot(listOf(spot), mapOf(spot.id to 90f), hour = 23, minutesOfDay = 22 * 60)
        assertEquals(spot.id, suggestion?.restaurant?.id)
        assertNull(suggestion?.meal)
    }

    @Test
    fun closestOpenSpotIsNullWithoutLocation() {
        assertNull(closestOpenSpot(restaurants, emptyMap(), hour = 12, minutesOfDay = 12 * 60))
    }
}
