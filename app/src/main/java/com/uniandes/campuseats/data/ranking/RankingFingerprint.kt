package com.uniandes.campuseats.data.ranking

object RankingFingerprint {

    private const val PRIME = 1_000_003L

    fun of(inputs: RankingInputs, window: RankingWindow): Long {
        var hash = window.ordinal.toLong() + 1
        if (window == RankingWindow.WEEK) hash = mix(hash, inputs.weekCutoff.hashCode().toLong())
        inputs.restaurants.forEach {
            hash = mix(hash, it.id.hashCode().toLong())
            hash = mix(hash, it.rating.hashCode().toLong())
            hash = mix(hash, it.reviews.toLong())
        }
        inputs.reviews.forEach {
            hash = mix(hash, it.id.hashCode().toLong())
            hash = mix(hash, it.restaurantId.hashCode().toLong())
            hash = mix(hash, it.date.hashCode().toLong())
            hash = mix(hash, it.ratings.hashCode().toLong())
        }
        inputs.crowding.toSortedMap().forEach { (id, levels) ->
            hash = mix(hash, id.hashCode().toLong())
            hash = mix(hash, levels.hashCode().toLong())
        }
        return hash
    }

    private fun mix(hash: Long, value: Long): Long = hash * PRIME + value
}
