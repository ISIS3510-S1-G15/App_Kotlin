package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Restaurant

object RankingSnapshots {

    fun restore(
        criterion: RankingCriterion,
        window: RankingWindow,
        stored: StoredRanking,
        previousIds: List<String>?,
        restaurants: List<Restaurant>
    ): RankingResult? {
        val byId = restaurants.associateBy { it.id }
        val items = stored.rows
            .mapNotNull { row -> byId[row.restaurantId]?.let { row to it } }
            .mapIndexed { index, (row, restaurant) ->
                RankedRestaurant(index + 1, restaurant, row.score, row.reviewCount, null)
            }
        if (items.isEmpty()) return null
        return RankingResult(
            criterion = criterion,
            window = window,
            items = RankingMovement.apply(items, previousIds),
            computedAt = stored.computedAt,
            fromSnapshot = true
        )
    }
}
