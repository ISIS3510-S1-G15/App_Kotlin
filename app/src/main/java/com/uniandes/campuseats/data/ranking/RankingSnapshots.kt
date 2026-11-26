package com.uniandes.campuseats.data.ranking

import androidx.collection.ArrayMap
import com.uniandes.campuseats.data.Restaurant

object RankingSnapshots {

    fun restore(
        criterion: RankingCriterion,
        window: RankingWindow,
        stored: StoredRanking,
        previousIds: List<String>?,
        restaurants: List<Restaurant>
    ): RankingResult? {
        val byId = ArrayMap<String, Restaurant>(restaurants.size)
        for (index in restaurants.indices) byId[restaurants[index].id] = restaurants[index]

        val rows = stored.rows
        val items = ArrayList<RankedRestaurant>(rows.size)
        for (index in rows.indices) {
            val row = rows[index]
            val restaurant = byId[row.restaurantId] ?: continue
            items.add(RankedRestaurant(items.size + 1, restaurant, row.score, row.reviewCount, null))
        }
        if (items.isEmpty()) return null
        return RankingResult(
            criterion = criterion,
            window = window,
            items = RankingMovement.apply(items, previousIds),
            computedAt = stored.computedAt,
            fromSnapshot = true,
            source = RankingSource.DISK
        )
    }
}
