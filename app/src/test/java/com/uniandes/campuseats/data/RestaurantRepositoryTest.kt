package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.remote.RestaurantCache
import com.uniandes.campuseats.data.remote.RestaurantRepository
import com.uniandes.campuseats.data.remote.parseRestaurants
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantRepositoryTest {

    private val payload = JSONObject(
        """
        {"restaurants":[
          {"id":"1","name":"Starbucks","location":"Plazoleta","category":"Café","image_key":"starbucks",
           "image_url":null,"rating":4.7,"review_count":10,"price":"$","wait_time":"2 min",
           "tags":["Coffee"],"hours":"6:30 AM – 11:00 PM","description":"d","map_x":55.0,"map_y":25.0,
           "latitude":4.6,"longitude":-74.0,
           "menu":[{"category":"Drinks","items":[{"name":"Latte","price":"$1","description":"x"}]}]},
          {"id":"4","name":"Cosechas","location":"ML","category":"Smoothies","image_key":null,
           "image_url":"https://img/x.jpg","rating":4.4,"review_count":2,"price":"$$","wait_time":"3 min",
           "tags":[],"hours":"6:30 AM – 8:00 PM","description":"d","map_x":60.0,"map_y":35.0,
           "latitude":4.6,"longitude":-74.0,"menu":[]}
        ]}
        """.trimIndent()
    )

    private class MemoryCache(var raw: String? = null) : RestaurantCache {
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

    private val imageFor: (String) -> Int? = { if (it == "starbucks") 42 else null }

    @Test
    fun parsesFieldsMenuAndImages() {
        val parsed = parseRestaurants(payload, imageFor)
        assertEquals(listOf("1", "4"), parsed.map { it.id })
        assertEquals(42, parsed[0].image)
        assertEquals("https://img/x.jpg", parsed[1].image)
        assertEquals("Latte", parsed[0].menu[0].items[0].name)
        assertEquals(10, parsed[0].reviews)
        assertEquals(55f, parsed[0].mapX)
    }

    @Test
    fun fetchStoresCacheAndCachedReadsItBack() = runTest {
        val cache = MemoryCache()
        val repository = RestaurantRepository(FakeApi(payload), cache, imageFor)
        assertTrue(repository.cached().isEmpty())
        assertEquals(2, repository.fetch().size)
        assertEquals(listOf("1", "4"), repository.cached().map { it.id })
    }

    @Test
    fun failedFetchKeepsCachedData() = runTest {
        val cache = MemoryCache(payload.toString())
        val repository = RestaurantRepository(FakeApi(null), cache, imageFor)
        val failed = runCatching { repository.fetch() }.isFailure
        assertTrue(failed)
        assertEquals(2, repository.cached().size)
    }

    @Test
    fun corruptCacheYieldsEmptyList() {
        val repository = RestaurantRepository(FakeApi(null), MemoryCache("not json"), imageFor)
        assertTrue(repository.cached().isEmpty())
    }
}
