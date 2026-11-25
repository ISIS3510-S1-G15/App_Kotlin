package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.restaurants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RankingMovementTest {

    private fun ranked(vararg ids: String) = ids.mapIndexed { index, id ->
        RankedRestaurant(index + 1, restaurants.first { it.id == id }, 4.0, 1, null)
    }

    @Test
    fun withoutBaselineNothingMoves() {
        val items = ranked("1", "2", "3")
        assertSame(items, RankingMovement.apply(items, null))
    }

    @Test
    fun climbingIsPositiveAndDroppingIsNegative() {
        val moved = RankingMovement.apply(ranked("3", "1", "2"), listOf("1", "2", "3"))
        assertEquals(listOf(2, -1, -1), moved.map { it.movement })
    }

    @Test
    fun samePositionIsZero() {
        val moved = RankingMovement.apply(ranked("1", "2"), listOf("1", "2"))
        assertEquals(listOf(0, 0), moved.map { it.movement })
    }

    @Test
    fun restaurantMissingFromBaselineHasNoMovement() {
        val moved = RankingMovement.apply(ranked("1", "4"), listOf("1", "2"))
        assertEquals(0, moved[0].movement)
        assertNull(moved[1].movement)
    }
}
