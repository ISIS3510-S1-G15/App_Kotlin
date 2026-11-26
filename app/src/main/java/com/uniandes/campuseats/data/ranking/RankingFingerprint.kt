package com.uniandes.campuseats.data.ranking

object RankingFingerprint {

    private const val PRIME = 1_000_003L

    fun of(inputs: RankingInputs, window: RankingWindow): Long {
        var hash = window.ordinal.toLong() + 1
        if (window == RankingWindow.MONTH) hash = mix(hash, inputs.windowCutoff.hashCode().toLong())

        val restaurants = inputs.restaurants
        for (index in restaurants.indices) {
            val restaurant = restaurants[index]
            hash = mix(hash, restaurant.id.hashCode().toLong())
            hash = mix(hash, restaurant.rating.hashCode().toLong())
            hash = mix(hash, restaurant.reviews.toLong())
        }

        val reviews = inputs.reviews
        for (index in reviews.indices) {
            val review = reviews[index]
            hash = mix(hash, review.id.hashCode().toLong())
            hash = mix(hash, review.restaurantId.hashCode().toLong())
            hash = mix(hash, review.date.hashCode().toLong())
            hash = mix(hash, review.ratings.hashCode().toLong())
        }

        var crowdingHash = 0L
        for ((id, levels) in inputs.crowding) {
            var entryHash = id.hashCode().toLong()
            for (index in levels.indices) entryHash = mix(entryHash, levels[index].toLong())
            crowdingHash += entryHash
        }
        return mix(hash, crowdingHash)
    }

    private fun mix(hash: Long, value: Long): Long = hash * PRIME + value
}
