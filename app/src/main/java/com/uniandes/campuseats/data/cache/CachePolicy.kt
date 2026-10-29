package com.uniandes.campuseats.data.cache

import com.uniandes.campuseats.net.attemptOrNull

interface DocumentCache {
    suspend fun load(key: String): String?
    suspend fun fetchedAt(key: String): Long?
    suspend fun save(key: String, raw: String, fetchedAt: Long)
    suspend fun clear(key: String)
}

object CacheTtl {
    const val CATALOG = 24 * 60 * 60 * 1000L
    const val RESTAURANTS = 60 * 60 * 1000L
    const val REVIEWS = 10 * 60 * 1000L
    const val REVIEW_SUMMARIES = 10 * 60 * 1000L
    const val OCCUPANCY = 2 * 60 * 1000L
}

object CacheKeys {
    const val SCHEMA_VERSION = 1
    const val RESTAURANTS = "restaurants.v$SCHEMA_VERSION"
    const val CATALOG = "catalog.v$SCHEMA_VERSION"
    const val REVIEW_SUMMARIES = "review_summaries.v$SCHEMA_VERSION"
    const val OCCUPANCY = "occupancy.v$SCHEMA_VERSION"

    fun reviews(restaurantId: String) = "reviews.v$SCHEMA_VERSION.$restaurantId"
}

class CachePolicy(private val clock: () -> Long = System::currentTimeMillis) {

    fun now(): Long = clock()

    fun isFresh(fetchedAt: Long?, ttlMs: Long): Boolean {
        if (fetchedAt == null || fetchedAt <= 0L) return false
        return clock() - fetchedAt in 0 until ttlMs
    }

    suspend fun isFresh(cache: DocumentCache, key: String, ttlMs: Long): Boolean =
        isFresh(cache.fetchedAt(key), ttlMs)

    suspend fun isFreshOrFalse(cache: DocumentCache, key: String, ttlMs: Long): Boolean =
        attemptOrNull { isFresh(cache, key, ttlMs) } == true

    suspend fun markFetched(cache: DocumentCache, key: String) {
        cache.save(key, "", now())
    }
}
