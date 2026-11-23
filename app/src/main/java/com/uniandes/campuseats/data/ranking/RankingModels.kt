package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review

enum class RankingCriterion(val label: String) {
    OVERALL("Overall"),
    TASTE("Taste"),
    SERVICE("Service"),
    VALUE("Value"),
    LESS_CROWDED("Less crowded")
}

enum class RankingWindow(val label: String) {
    WEEK("This week"),
    ALL_TIME("All time")
}

data class RankedRestaurant(
    val position: Int,
    val restaurant: Restaurant,
    val score: Double,
    val reviewCount: Int,
    val movement: Int?
)

data class RankingResult(
    val criterion: RankingCriterion,
    val window: RankingWindow,
    val items: List<RankedRestaurant>,
    val computedAt: Long,
    val fromSnapshot: Boolean
)

data class RankingInputs(
    val restaurants: List<Restaurant>,
    val reviews: List<Review>,
    val crowding: Map<String, List<Int>>,
    val weekCutoff: String
)

data class SnapshotRow(
    val restaurantId: String,
    val score: Double,
    val reviewCount: Int
)

data class StoredRanking(
    val rows: List<SnapshotRow>,
    val computedAt: Long
) {
    val restaurantIds: List<String> get() = rows.map { it.restaurantId }
}
