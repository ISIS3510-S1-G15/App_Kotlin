package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.CachedDocument
import com.uniandes.campuseats.data.cache.MemoryCache
import com.uniandes.campuseats.data.cache.TieredDocumentCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCacheTest {

    @Test
    fun evictsTheLeastRecentlyUsedEntry() {
        val cache = MemoryCache<String, Int>(maxEntries = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a") // "a" pasa a ser la mas reciente
        cache.put("c", 3)
        assertEquals(1, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(3, cache.get("c"))
        assertEquals(1L, cache.stats.value.evictions)
    }

    @Test
    fun entriesExpireAfterTheirTtl() {
        val clock = FakeClock()
        val cache = MemoryCache<String, Int>(maxEntries = 4, clock = clock)
        cache.put("a", 1, ttlMs = 1_000)
        assertEquals(1, cache.get("a"))
        clock.now += 1_000
        assertNull(cache.get("a"))
        assertEquals(1L, cache.stats.value.expirations)
        assertEquals(0, cache.size)
    }

    @Test
    fun countsHitsAndMisses() {
        val cache = MemoryCache<String, Int>(maxEntries = 4)
        cache.put("a", 1)
        cache.get("a")
        cache.get("a")
        cache.get("zzz")
        val stats = cache.stats.value
        assertEquals(2L, stats.hits)
        assertEquals(1L, stats.misses)
        assertEquals(2.0 / 3.0, stats.hitRate, 0.0001)
    }

    @Test
    fun concurrentLoadsOfTheSameKeyShareASingleRequest() = runTest {
        val cache = MemoryCache<String, String>(maxEntries = 4)
        val gate = CompletableDeferred<Unit>()
        var loads = 0
        val loader: suspend () -> String = {
            loads++
            gate.await()
            "value"
        }
        val first = async { cache.getOrLoad("k", loader = loader) }
        val second = async { cache.getOrLoad("k", loader = loader) }
        val third = async { cache.getOrLoad("k", loader = loader) }
        gate.complete(Unit)
        assertEquals(listOf("value", "value", "value"), listOf(first.await(), second.await(), third.await()))
        assertEquals(1, loads)
        assertEquals("value", cache.get("k"))
    }

    @Test
    fun aFailedLoadIsNotCachedAndCanBeRetried() = runTest {
        val cache = MemoryCache<String, String>(maxEntries = 4)
        val failure = runCatching { cache.getOrLoad("k") { error("offline") } }
        assertTrue(failure.isFailure)
        assertEquals("ok", cache.getOrLoad("k") { "ok" })
    }

    @Test
    fun resizeAndTrimDropOldestEntries() {
        val cache = MemoryCache<Int, Int>(maxEntries = 8)
        (1..8).forEach { cache.put(it, it) }
        cache.trimTo(0.5f)
        assertEquals(4, cache.size)
        assertNull(cache.peek(1))
        assertEquals(8, cache.peek(8))
        cache.resize(2)
        assertEquals(2, cache.size)
        cache.clear()
        assertEquals(0, cache.size)
    }

    @Test
    fun invalidateWhereRemovesMatchingKeys() {
        val cache = MemoryCache<String, Int>(maxEntries = 8)
        cache.put("reviews.1", 1)
        cache.put("reviews.2", 2)
        cache.put("catalog", 3)
        cache.invalidateWhere { it.startsWith("reviews.") }
        assertEquals(1, cache.size)
        assertEquals(3, cache.peek("catalog"))
    }

    // ---- cache de dos niveles ----

    @Test
    fun tieredCacheReadsMemoryFirstAndPromotesDiskHits() = runTest {
        val disk = FakeDocumentCache().apply { put("k", "payload", 42L) }
        val memory = MemoryCache<String, CachedDocument>(maxEntries = 4)
        val cache = TieredDocumentCache(disk, memory)

        assertEquals("payload", cache.load("k")) // viene de disco y se promueve a memoria
        assertEquals("payload", memory.peek("k")?.payload)
        assertEquals(42L, cache.fetchedAt("k"))

        disk.clear("k")
        assertEquals("payload", cache.load("k")) // ya no hace falta el disco
    }

    @Test
    fun memoryOnlyModeNeverWritesToDisk() = runTest {
        val disk = FakeDocumentCache()
        val memory = MemoryCache<String, CachedDocument>(maxEntries = 4)
        var persist = false
        val cache = TieredDocumentCache(disk, memory) { persist }

        cache.save("k", "v", 1L)
        assertFalse(disk.has("k"))
        assertEquals("v", cache.load("k"))

        persist = true
        cache.save("k2", "v2", 2L)
        assertTrue(disk.has("k2"))
    }

    @Test
    fun clearRemovesFromBothLevels() = runTest {
        val disk = FakeDocumentCache()
        val memory = MemoryCache<String, CachedDocument>(maxEntries = 4)
        val cache = TieredDocumentCache(disk, memory)
        cache.save("k", "v", 1L)
        cache.clear("k")
        assertNull(cache.load("k"))
        assertFalse(disk.has("k"))
    }
}
