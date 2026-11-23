package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.outbox.OutboxEntry
import com.uniandes.campuseats.data.outbox.OutboxFlusher
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.data.outbox.OutboxStore
import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.IdempotencyKey
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.coroutineContext

class OutboxBackoffTest {

    private class SchedulingOutbox : OutboxStore {
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
            val i = entries.indexOfFirst { it.id == id }
            entries[i] = entries[i].copy(attempts = entries[i].attempts + 1)
        }

        override suspend fun scheduleRetry(id: Long, notBefore: Long) {
            val i = entries.indexOfFirst { it.id == id }
            entries[i] = entries[i].copy(nextAttemptAt = notBefore)
        }

        override suspend fun count() = entries.size
        override suspend fun clear() = entries.clear()
    }

    private class RecordingApi(var status: Int? = null) : RemoteApi {
        val calls = mutableListOf<String>()
        val idempotencyKeys = mutableListOf<String?>()

        private suspend fun handle(path: String): JSONObject {
            calls.add(path)
            idempotencyKeys.add(coroutineContext[IdempotencyKey]?.value)
            status?.let { throw ApiException(it, "") }
            return JSONObject()
        }

        override suspend fun get(path: String) = handle(path)
        override suspend fun post(path: String, body: Map<String, Any>) = handle(path)
        override suspend fun put(path: String, body: Map<String, Any>?) = handle(path)
        override suspend fun delete(path: String) = handle(path)
    }

    private val occupancy = OutboxRequest(OutboxKind.OCCUPANCY, "POST", "/occupancy/reports", mapOf("level" to 1))

    @Test
    fun serverErrorSchedulesAnExponentialBackoffAndSkipsUntilItElapses() = runTest {
        val clock = FakeClock(now = 10_000L)
        val store = SchedulingOutbox().apply { enqueue(occupancy) }
        val api = RecordingApi(status = 503)
        val flusher = OutboxFlusher(api, store, clock = clock)

        flusher.flush()
        assertEquals(1, api.calls.size)
        assertEquals(10_000L + OutboxFlusher.backoffMs(1), store.entries.single().nextAttemptAt)

        flusher.flush() // todavia dentro del backoff: no toca la red
        assertEquals(1, api.calls.size)

        clock.now += OutboxFlusher.backoffMs(1)
        flusher.flush()
        assertEquals(2, api.calls.size)
        assertEquals(clock.now + OutboxFlusher.backoffMs(2), store.entries.single().nextAttemptAt)
    }

    @Test
    fun ignoreBackoffForcesAnImmediateRetry() = runTest {
        val clock = FakeClock()
        val store = SchedulingOutbox().apply { enqueue(occupancy) }
        val api = RecordingApi(status = 500)
        val flusher = OutboxFlusher(api, store, clock = clock)
        flusher.flush()
        flusher.flush(ignoreBackoff = true)
        assertEquals(2, api.calls.size)
    }

    @Test
    fun backoffGrowsAndIsCapped() {
        assertTrue(OutboxFlusher.backoffMs(2) > OutboxFlusher.backoffMs(1))
        assertEquals(OutboxFlusher.backoffMs(10), OutboxFlusher.backoffMs(50))
    }

    @Test
    fun unauthorizedReportsAuthExpiredAndKeepsEverythingQueued() = runTest {
        val store = SchedulingOutbox().apply { enqueue(occupancy) }
        val result = OutboxFlusher(RecordingApi(status = 401), store).flush()
        assertTrue(result.authExpired)
        assertEquals(1, result.remaining)
        assertEquals(0, result.dropped)
    }

    @Test
    fun everyRetryReusesTheSameIdempotencyKey() = runTest {
        val store = SchedulingOutbox().apply { enqueue(occupancy) }
        val api = RecordingApi(status = 500)
        val flusher = OutboxFlusher(api, store, clock = FakeClock())
        flusher.flush()
        flusher.flush(ignoreBackoff = true)
        assertEquals(2, api.idempotencyKeys.size)
        assertEquals(occupancy.requestId, api.idempotencyKeys[0])
        assertEquals(api.idempotencyKeys[0], api.idempotencyKeys[1])
    }
}
