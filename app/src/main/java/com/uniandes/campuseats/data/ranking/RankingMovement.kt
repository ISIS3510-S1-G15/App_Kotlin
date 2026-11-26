package com.uniandes.campuseats.data.ranking

import androidx.collection.MutableObjectIntMap

object RankingMovement {

    private const val ABSENT = 0

    fun apply(items: List<RankedRestaurant>, baselineIds: List<String>?): List<RankedRestaurant> {
        if (baselineIds == null) return items
        val previousPosition = MutableObjectIntMap<String>(baselineIds.size)
        for (index in baselineIds.indices) previousPosition[baselineIds[index]] = index + 1

        val moved = ArrayList<RankedRestaurant>(items.size)
        for (index in items.indices) {
            val item = items[index]
            val before = previousPosition.getOrDefault(item.restaurant.id, ABSENT)
            moved.add(
                when {
                    before != ABSENT -> item.copy(movement = before - item.position)
                    item.movement == null -> item
                    else -> item.copy(movement = null)
                }
            )
        }
        return moved
    }
}
