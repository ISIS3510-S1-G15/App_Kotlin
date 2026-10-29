package com.uniandes.campuseats.data.local

import com.uniandes.campuseats.data.cache.DocumentCache

class RoomDocumentCache(private val store: LocalStore) : DocumentCache {

    override suspend fun load(key: String): String? = store.loadDocument(key)?.payload

    override suspend fun fetchedAt(key: String): Long? = store.loadDocument(key)?.fetchedAt

    override suspend fun save(key: String, raw: String, fetchedAt: Long) =
        store.saveDocument(key, raw, fetchedAt)

    override suspend fun clear(key: String) = store.deleteDocument(key)
}
