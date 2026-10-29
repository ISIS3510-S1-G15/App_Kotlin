package com.uniandes.campuseats.data.local

import com.uniandes.campuseats.data.remote.CatalogCache
import com.uniandes.campuseats.data.remote.RestaurantCache

class RoomRestaurantCache(
    private val store: LocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) : RestaurantCache {

    override suspend fun load(): String? = store.loadDocument(KEY)?.payload

    override suspend fun save(raw: String) = store.saveDocument(KEY, raw, clock())

    private companion object {
        const val KEY = "restaurants"
    }
}

class RoomCatalogCache(
    private val store: LocalStore,
    private val clock: () -> Long = System::currentTimeMillis
) : CatalogCache {

    override suspend fun load(): String? = store.loadDocument(KEY)?.payload

    override suspend fun save(raw: String) = store.saveDocument(KEY, raw, clock())

    private companion object {
        const val KEY = "catalog"
    }
}
