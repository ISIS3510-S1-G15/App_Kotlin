package com.uniandes.campuseats.data.ranking

object RankingMovement {

    fun apply(items: List<RankedRestaurant>, baselineIds: List<String>?): List<RankedRestaurant> {
        if (baselineIds == null) return items
        val previousPosition = baselineIds.withIndex().associate { (index, id) -> id to index + 1 }
        return items.map { item ->
            val before = previousPosition[item.restaurant.id]
            item.copy(movement = before?.let { it - item.position })
        }
    }
}
