package com.uniandes.campuseats

import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.MealRules
import com.uniandes.campuseats.data.greetingFor
import com.uniandes.campuseats.data.restaurants
import com.uniandes.campuseats.data.testMealRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealContextTest {

    private val rules = testMealRules
    private val lunchMoment = rules.momentAt(12)!!

    @Test
    fun mealSlotFollowsTheHourOfTheDay() {
        assertEquals("Breakfast", rules.mealSlotFor(7))
        assertEquals("Mid-morning snack", rules.mealSlotFor(10))
        assertEquals("Lunch", rules.mealSlotFor(12))
        assertEquals("Afternoon snack", rules.mealSlotFor(16))
        assertEquals("Dinner", rules.mealSlotFor(20))
        assertEquals("Late night", rules.mealSlotFor(23))
        assertEquals("Late night", rules.mealSlotFor(3))
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
        assertNull(rules.momentAt(23))
        assertNull(rules.momentAt(3))
        assertEquals("Lunch", rules.momentAt(12)?.label)
    }

    @Test
    fun walkingMinutesRoundUpAndNeverDropBelowOne() {
        assertEquals(1, rules.walkingMinutes(10f))
        assertEquals(1, rules.walkingMinutes(80f))
        assertEquals(2, rules.walkingMinutes(81f))
        assertEquals(5, rules.walkingMinutes(350f))
    }

    @Test
    fun campusRadiusIs600Meters() {
        assertTrue(rules.isOnCampus(600f))
        assertFalse(rules.isOnCampus(601f))
    }

    @Test
    fun closestOpenSpotIgnoresClosedPlacesAndPrefersTheMealThatFits() {
        val lunch = restaurants.first { lunchMoment.fits(it.category) }
        val other = restaurants.first { it.id != lunch.id && !lunchMoment.fits(it.category) }
        val distances = mapOf(other.id to 40f, lunch.id to 200f)
        val suggestion = rules.closestOpenSpot(listOf(other, lunch), distances, hour = 12, minutesOfDay = 12 * 60)
        assertEquals(lunch.id, suggestion?.restaurant?.id)
        assertEquals(lunchMoment, suggestion?.meal)
    }

    @Test
    fun closestOpenSpotFallsBackToTheNearestOpenOneWhenNoMealFits() {
        val spot = restaurants.first()
        val suggestion = rules.closestOpenSpot(listOf(spot), mapOf(spot.id to 90f), hour = 23, minutesOfDay = 22 * 60)
        assertEquals(spot.id, suggestion?.restaurant?.id)
        assertNull(suggestion?.meal)
    }

    @Test
    fun emptyCatalogYieldsNoCampusAndNoSlot() {
        val empty = MealRules(Catalog.Empty)
        assertFalse(empty.isOnCampus(0f))
        assertEquals("", empty.mealSlotFor(12))
        assertNull(empty.momentAt(12))
    }

    @Test
    fun closestOpenSpotIsNullWithoutLocation() {
        assertNull(rules.closestOpenSpot(restaurants, emptyMap(), hour = 12, minutesOfDay = 12 * 60))
    }
}
