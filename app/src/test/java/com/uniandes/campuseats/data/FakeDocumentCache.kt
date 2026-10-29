package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.cache.DocumentCache

class FakeDocumentCache : DocumentCache {
    private val documents = mutableMapOf<String, Pair<String, Long>>()

    fun put(key: String, raw: String, fetchedAt: Long) {
        documents[key] = raw to fetchedAt
    }

    fun has(key: String) = key in documents

    override suspend fun load(key: String): String? = documents[key]?.first

    override suspend fun fetchedAt(key: String): Long? = documents[key]?.second

    override suspend fun save(key: String, raw: String, fetchedAt: Long) {
        documents[key] = raw to fetchedAt
    }

    override suspend fun clear(key: String) {
        documents.remove(key)
    }
}

class FakeClock(var now: Long = 1_000_000L) : () -> Long {
    override fun invoke(): Long = now
}
