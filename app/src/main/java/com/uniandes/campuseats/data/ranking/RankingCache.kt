package com.uniandes.campuseats.data.ranking

import androidx.collection.LruCache
import java.util.concurrent.atomic.AtomicInteger

class RankingCache(maxEntries: Int = DEFAULT_ENTRIES) {

    private val entries = LruCache<String, RankingResult>(maxEntries)
    private val hitCount = AtomicInteger()
    private val missCount = AtomicInteger()

    val hits: Int get() = hitCount.get()
    val misses: Int get() = missCount.get()

    fun get(criterion: RankingCriterion, window: RankingWindow, fingerprint: Long): RankingResult? {
        val found = entries.get(keyOf(criterion, window, fingerprint))
        if (found == null) missCount.incrementAndGet() else hitCount.incrementAndGet()
        return found
    }

    fun put(result: RankingResult, fingerprint: Long) {
        entries.put(keyOf(result.criterion, result.window, fingerprint), result)
    }

    fun putAll(results: Collection<RankingResult>, fingerprint: Long) {
        results.forEach { put(it, fingerprint) }
    }

    fun size(): Int = entries.size()

    fun clear() {
        entries.evictAll()
    }

    fun trim(fraction: Float) {
        entries.trimToSize((entries.size() * fraction).toInt())
    }

    private fun keyOf(criterion: RankingCriterion, window: RankingWindow, fingerprint: Long) =
        "${criterion.name}|${window.name}|$fingerprint"

    companion object {
        const val DEFAULT_ENTRIES = 10
    }
}
