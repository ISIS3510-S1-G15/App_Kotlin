package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.remote.CatalogCache
import com.uniandes.campuseats.data.remote.CatalogRepository
import com.uniandes.campuseats.data.remote.parseCatalog
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
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

    private class MemoryCache(var raw: String? = null) : CatalogCache {
        override fun load() = raw
        override fun save(raw: String) {
            this.raw = raw
        }
    }

    private class FakeApi(private val response: JSONObject?) : RemoteApi {
        override suspend fun get(path: String): JSONObject = response ?: throw IOException("offline")
        override suspend fun post(path: String, body: Map<String, Any>) = JSONObject()
        override suspend fun put(path: String, body: Map<String, Any>?) = JSONObject()
        override suspend fun delete(path: String) = JSONObject()
    }

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
        val cache = MemoryCache()
        assertTrue(CatalogRepository(FakeApi(payload), cache).fetch().labels("filter_prices").size == 2)
        val offline = CatalogRepository(FakeApi(null), cache)
        assertTrue(runCatching { offline.fetch() }.isFailure)
        assertEquals(2, offline.cached().labels("filter_prices").size)
    }

    @Test
    fun corruptCacheYieldsEmptyCatalog() {
        assertTrue(CatalogRepository(FakeApi(null), MemoryCache("not json")).cached().isEmpty)
    }
}
