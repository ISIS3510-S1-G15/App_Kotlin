package com.uniandes.campuseats.data.ranking

import java.util.concurrent.atomic.AtomicInteger

interface CacheBacking {
    operator fun get(key: String): RankingResult?
    fun put(key: String, value: RankingResult)
    fun evictAll()
    fun sizeUnits(): Int
    fun entryCount(): Int
    fun trimToSize(units: Int)
}

class AndroidLruBacking(maxKilobytes: Int) : CacheBacking {

    private val lru = object : android.util.LruCache<String, RankingResult>(maxKilobytes) {
        override fun sizeOf(key: String, value: RankingResult): Int = RankingCache.estimateKilobytes(value)
    }

    override fun get(key: String): RankingResult? = lru.get(key)

    override fun put(key: String, value: RankingResult) {
        lru.put(key, value)
    }

    override fun evictAll() = lru.evictAll()

    override fun sizeUnits(): Int = lru.size()

    override fun entryCount(): Int = lru.snapshot().size

    override fun trimToSize(units: Int) = lru.trimToSize(units)
}

class RankingCache(private val backing: CacheBacking = AndroidLruBacking(deviceCacheKilobytes())) {

    private val hitCount = AtomicInteger()
    private val missCount = AtomicInteger()

    val hits: Int get() = hitCount.get()
    val misses: Int get() = missCount.get()

    fun get(criterion: RankingCriterion, window: RankingWindow, fingerprint: Long): RankingResult? {
        val found = backing[keyOf(criterion, window, fingerprint)]
        if (found == null) missCount.incrementAndGet() else hitCount.incrementAndGet()
        return found
    }

    fun put(result: RankingResult, fingerprint: Long) {
        backing.put(keyOf(result.criterion, result.window, fingerprint), result)
    }

    fun putAll(results: Collection<RankingResult>, fingerprint: Long) {
        for (result in results) put(result, fingerprint)
    }

    fun size(): Int = backing.entryCount()

    fun clear() {
        backing.evictAll()
    }

    fun trim(fraction: Float) {
        backing.trimToSize((backing.sizeUnits() * fraction).toInt())
    }

    private fun keyOf(criterion: RankingCriterion, window: RankingWindow, fingerprint: Long) =
        "${criterion.name}|${window.name}|$fingerprint"

    companion object {
        const val DEFAULT_ENTRIES = 10
        private const val BYTES_PER_ROW = 192
        private const val BYTES_PER_RESULT = 256
        private const val MAX_CACHE_KILOBYTES = 4096
        private const val MEMORY_FRACTION = 8

        fun estimateKilobytes(result: RankingResult): Int =
            ((result.items.size * BYTES_PER_ROW + BYTES_PER_RESULT) / 1024).coerceAtLeast(1)

        fun deviceCacheKilobytes(): Int {
            val maxMemoryKilobytes = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return (maxMemoryKilobytes / MEMORY_FRACTION).coerceIn(DEFAULT_ENTRIES, MAX_CACHE_KILOBYTES)
        }
    }
}
