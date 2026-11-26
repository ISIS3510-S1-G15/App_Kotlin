package com.uniandes.campuseats.data.ranking

class MapBacking(private val maxEntries: Int = RankingCache.DEFAULT_ENTRIES) : CacheBacking {

    private val entries = object : LinkedHashMap<String, RankingResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RankingResult>): Boolean = size > maxEntries
    }

    override fun get(key: String): RankingResult? = entries[key]

    override fun put(key: String, value: RankingResult) {
        entries[key] = value
    }

    override fun evictAll() = entries.clear()

    override fun sizeUnits(): Int = entries.size

    override fun entryCount(): Int = entries.size

    override fun trimToSize(units: Int) {
        val iterator = entries.entries.iterator()
        while (entries.size > units && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }
}
