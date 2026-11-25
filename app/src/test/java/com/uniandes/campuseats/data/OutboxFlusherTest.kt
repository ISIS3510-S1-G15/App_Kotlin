package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.outbox.OutboxCodec
import com.uniandes.campuseats.data.outbox.OutboxEntry
import com.uniandes.campuseats.data.outbox.OutboxFlusher
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.data.outbox.OutboxStore
import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.RemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxFlusherTest {

    private class MemoryOutbox : OutboxStore {
        private var nextId = 1L
        val entries = mutableListOf<OutboxEntry>()

        override suspend fun enqueue(request: OutboxRequest) {
            entries.add(OutboxEntry(nextId++, request, 0))
        }

        override suspend fun pending() = entries.toList()

        override suspend fun remove(id: Long) {
            entries.removeAll { it.id == id }
        }

        override suspend fun recordAttempt(id: Long) {
            val index = entries.indexOfFirst { it.id == id }
            entries[index] = entries[index].copy(attempts = entries[index].attempts + 1)
        }

        override suspend fun count() = entries.size

        override suspend fun clear() {
            entries.clear()
        }
    }

    private class ScriptedApi(private val failures: MutableMap<String, Exception> = mutableMapOf()) : RemoteApi {
        val calls = mutableListOf<String>()

        fun failOn(path: String, error: Exception) {
            failures[path] = error
        }

        fun recover() = failures.clear()

        private fun handle(method: String, path: String): JSONObject {
            calls.add("$method $path")
            failures[path]?.let { throw it }
            return JSONObject()
        }

        override suspend fun get(path: String) = handle("GET", path)
        override suspend fun post(path: String, body: Map<String, Any>) = handle("POST", path)
        override suspend fun put(path: String, body: Map<String, Any>?) = handle("PUT", path)
        override suspend fun delete(path: String) = handle("DELETE", path)
    }

    private val saved = OutboxRequest(OutboxKind.SAVED, "PUT", "/me/saved/1")
    private val unsaved = OutboxRequest(OutboxKind.SAVED, "DELETE", "/me/saved/1")
    private val occupancy = OutboxRequest(OutboxKind.OCCUPANCY, "POST", "/occupancy/reports", mapOf("level" to 2))
    private val analytics = OutboxRequest(OutboxKind.ANALYTICS, "POST", "/analytics/filter-usage", mapOf("filter" to "x"))

    @Test
    fun sendsEverythingInOrderAndEmptiesTheQueue() = runTest {
        val store = MemoryOutbox().apply { enqueue(saved); enqueue(occupancy); enqueue(unsaved) }
        val api = ScriptedApi()
        val result = OutboxFlusher(api, store).flush()
        // Las categorias se envian en paralelo; el orden solo se garantiza dentro de cada una
        assertEquals(listOf("PUT /me/saved/1", "DELETE /me/saved/1"), api.calls.filter { it.contains("/me/saved") })
        assertEquals(1, api.calls.count { it == "POST /occupancy/reports" })
        assertEquals(3, api.calls.size)
        assertEquals(setOf(OutboxKind.SAVED, OutboxKind.OCCUPANCY), result.sentKinds)
        assertEquals(0, result.remaining)
        assertEquals(0, result.dropped)
    }

    @Test
    fun aNetworkFailureOnlyStopsItsOwnCategory() = runTest {
        val store = MemoryOutbox().apply { enqueue(saved); enqueue(occupancy); enqueue(analytics) }
        val api = ScriptedApi().apply { failOn("/occupancy/reports", IOException("offline")) }
        val result = OutboxFlusher(api, store).flush()
        assertEquals(setOf(OutboxKind.SAVED, OutboxKind.ANALYTICS), result.sentKinds)
        assertEquals(1, result.remaining)
        assertEquals(OutboxKind.OCCUPANCY, store.entries.single().request.kind)
        assertEquals(0, store.entries.single().attempts)
    }

    @Test
    fun deliversTheRemainingEntriesOnceTheNetworkReturns() = runTest {
        val store = MemoryOutbox().apply { enqueue(saved); enqueue(occupancy) }
        val api = ScriptedApi().apply { failOn("/me/saved/1", IOException("offline")) }
        val flusher = OutboxFlusher(api, store)
        // occupancy no depende de saved, asi que sale aunque saved siga offline
        assertEquals(1, flusher.flush().remaining)
        api.recover()
        val result = flusher.flush()
        assertEquals(0, result.remaining)
        assertEquals(setOf(OutboxKind.SAVED), result.sentKinds)
    }

    @Test
    fun rejectedRequestsAreDroppedWithoutBlockingTheQueue() = runTest {
        val store = MemoryOutbox().apply { enqueue(saved); enqueue(occupancy) }
        val api = ScriptedApi().apply { failOn("/me/saved/1", ApiException(404, "")) }
        val result = OutboxFlusher(api, store).flush()
        assertEquals(1, result.dropped)
        assertEquals(setOf(OutboxKind.OCCUPANCY), result.sentKinds)
        assertEquals(0, result.remaining)
    }

    @Test
    fun unauthorizedKeepsTheQueueIntact() = runTest {
        val store = MemoryOutbox().apply { enqueue(saved); enqueue(occupancy) }
        val api = ScriptedApi().apply { failOn("/me/saved/1", ApiException(401, "")) }
        val result = OutboxFlusher(api, store).flush()
        assertEquals(0, result.dropped)
        assertTrue(result.authExpired)
        assertEquals(1, result.remaining) // la peticion con 401 se conserva; la otra categoria si sale
        assertEquals(OutboxKind.SAVED, store.entries.single().request.kind)
        assertEquals(0, store.entries.single().attempts)
    }

    @Test
    fun serverErrorsCountAttemptsAndDropAfterTheLimit() = runTest {
        val store = MemoryOutbox().apply { enqueue(occupancy); enqueue(analytics) }
        val api = ScriptedApi().apply { failOn("/occupancy/reports", ApiException(500, "")) }
        val flusher = OutboxFlusher(api, store, maxAttempts = 3)
        // la analitica (otra categoria) sale en el primer flush; occupancy se queda reintentando
        assertEquals(1, flusher.flush(ignoreBackoff = true).remaining)
        assertEquals(1, store.entries.single().attempts)
        assertEquals(1, flusher.flush(ignoreBackoff = true).remaining)
        val last = flusher.flush(ignoreBackoff = true)
        assertEquals(1, last.dropped)
        assertEquals(0, last.remaining)
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun emptyQueueDoesNothing() = runTest {
        val api = ScriptedApi()
        val result = OutboxFlusher(api, MemoryOutbox()).flush()
        assertTrue(api.calls.isEmpty())
        assertEquals(0, result.remaining)
        assertTrue(!result.sent)
    }

    @Test
    fun codecRoundTripsNestedBodies() {
        val body = mapOf<String, Any>(
            "tags" to listOf("Vegan", "Fast Food"),
            "level" to 2,
            "ok" to true,
            "nested" to mapOf("a" to listOf(1, 2))
        )
        assertEquals(body, OutboxCodec.decode(OutboxCodec.encode(body)))
        assertNull(OutboxCodec.decode(OutboxCodec.encode(null)))
        assertNull(OutboxCodec.decode("not json"))
    }
}
