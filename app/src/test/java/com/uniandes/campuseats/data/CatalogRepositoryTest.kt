package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.remote.CatalogRepository
import com.uniandes.campuseats.data.remote.parseCatalog
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {

    private val payload = JSONObject(
        """
        {"groups":{
          "filter_prices":[
            {"value":"$","label":"$","detail":null,"emoji":null},
            {"value":"$$","label":"$$","detail":null,"emoji":null}],
          "crowding_levels":[
            {"value":"1","label":"Empty","detail":"Plenty of seats","emoji":"x"}]
        }}
        """.trimIndent()
    )

    private class FakeApi(private val response: JSONObject?) : RemoteApi {
        var calls = 0
        override suspend fun get(path: String): JSONObject {
            calls++
            return response ?: throw IOException("offline")
        }
        override suspend fun post(path: String, body: Map<String, Any>) = JSONObject()
        override suspend fun put(path: String, body: Map<String, Any>?) = JSONObject()
        override suspend fun delete(path: String) = JSONObject()
    }

    private val clock = FakeClock()
    private val policy = CachePolicy(clock)

    private fun repository(api: RemoteApi, cache: FakeDocumentCache) = CatalogRepository(api, cache, policy)

    @Test
    fun parsesGroupsKeepingOrderAndOptionalFields() {
        val catalog = parseCatalog(payload)
        assertEquals(listOf("$", "$$"), catalog.labels("filter_prices"))
        val level = catalog.options("crowding_levels").single()
        assertEquals("Plenty of seats", level.detail)
        assertEquals("Empty", catalog.label("crowding_levels", "1"))
        assertNull(catalog.options("filter_prices")[0].detail)
        assertTrue(catalog.options("missing").isEmpty())
    }

    @Test
    fun fetchStoresCacheAndFailureKeepsIt() = runTest {
        val cache = FakeDocumentCache()
        assertTrue(repository(FakeApi(payload), cache).fetch().labels("filter_prices").size == 2)
        assertEquals(clock.now, cache.fetchedAt(CacheKeys.CATALOG))
        val offline = repository(FakeApi(null), cache)
        assertTrue(runCatching { offline.fetch() }.isFailure)
        assertEquals(2, offline.cached().labels("filter_prices").size)
    }

    @Test
    fun corruptCacheYieldsEmptyCatalogAndIsDiscarded() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.CATALOG, "not json", clock.now) }
        assertTrue(repository(FakeApi(null), cache).cached().isEmpty)
        assertFalse(cache.has(CacheKeys.CATALOG))
    }

    @Test
    fun revalidateRespectsTheDailyTtl() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.CATALOG, payload.toString(), clock.now) }
        val api = FakeApi(payload)
        clock.now += CacheTtl.CATALOG - 1
        assertNull(repository(api, cache).revalidate())
        assertEquals(0, api.calls)
        clock.now += 1
        assertNotNull(repository(api, cache).revalidate())
        assertEquals(1, api.calls)
    }

    @Test
    fun forceRevalidateIgnoresFreshness() = runTest {
        val cache = FakeDocumentCache().apply { put(CacheKeys.CATALOG, payload.toString(), clock.now) }
        val api = FakeApi(payload)
        assertNotNull(repository(api, cache).revalidate(force = true))
        assertEquals(1, api.calls)
    }
}
