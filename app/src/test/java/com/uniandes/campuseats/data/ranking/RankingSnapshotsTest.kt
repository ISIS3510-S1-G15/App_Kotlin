package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.restaurants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingSnapshotsTest {

    private val stored = StoredRanking(
        rows = listOf(SnapshotRow("2", 4.5, 10), SnapshotRow("1", 4.0, 8), SnapshotRow("3", 3.5, 4)),
        computedAt = 1234L
    )

    @Test
    fun restoresOrderScoresAndTimestamp() {
        val result = RankingSnapshots.restore(RankingCriterion.TASTE, RankingWindow.WEEK, stored, null, restaurants)!!
        assertEquals(listOf("2", "1", "3"), result.items.map { it.restaurant.id })
        assertEquals(listOf(1, 2, 3), result.items.map { it.position })
        assertEquals(4.5, result.items.first().score, 0.0)
        assertEquals(1234L, result.computedAt)
        assertTrue(result.fromSnapshot)
    }

    @Test
    fun skipsRestaurantsThatNoLongerExist() {
        val withGhost = stored.copy(rows = listOf(SnapshotRow("gone", 5.0, 1)) + stored.rows)
        val result = RankingSnapshots.restore(RankingCriterion.TASTE, RankingWindow.WEEK, withGhost, null, restaurants)!!
        assertEquals(listOf("2", "1", "3"), result.items.map { it.restaurant.id })
        assertEquals(listOf(1, 2, 3), result.items.map { it.position })
    }

    @Test
    fun returnsNullWhenNothingMatches() {
        val result = RankingSnapshots.restore(RankingCriterion.TASTE, RankingWindow.WEEK, stored, null, emptyList())
        assertNull(result)
    }

    @Test
    fun appliesMovementAgainstPreviousSnapshot() {
        val result = RankingSnapshots.restore(
            RankingCriterion.TASTE, RankingWindow.WEEK, stored, listOf("1", "2", "3"), restaurants
        )!!
        assertEquals(listOf(1, -1, 0), result.items.map { it.movement })
    }
}
