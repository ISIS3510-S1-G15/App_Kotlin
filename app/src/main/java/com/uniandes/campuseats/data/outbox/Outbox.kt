package com.uniandes.campuseats.data.outbox

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object OutboxKind {
    const val SAVED = "saved"
    const val OCCUPANCY = "occupancy"
    const val REVIEW = "review"
    const val ANALYTICS = "analytics"
}

data class OutboxRequest(
    val kind: String,
    val method: String,
    val path: String,
    val body: Map<String, Any>? = null,
    /** Llave de idempotencia: el servidor puede ignorar un reenvío si ya procesó esta misma petición. */
    val requestId: String = UUID.randomUUID().toString()
)

data class OutboxEntry(
    val id: Long,
    val request: OutboxRequest,
    val attempts: Int,
    /** No reintentar antes de este instante (backoff exponencial). 0 = disponible ya. */
    val nextAttemptAt: Long = 0L
)

interface OutboxStore {
    suspend fun enqueue(request: OutboxRequest)
    suspend fun pending(): List<OutboxEntry>
    suspend fun remove(id: Long)
    suspend fun recordAttempt(id: Long)
    /** Aplaza la entrada hasta [notBefore] (backoff). Las implementaciones sin backoff pueden ignorarlo. */
    suspend fun scheduleRetry(id: Long, notBefore: Long) {}
    suspend fun count(): Int
    suspend fun clear()
}

object OutboxCodec {

    fun encode(body: Map<String, Any>?): String? = body?.let { JSONObject(it).toString() }

    fun decode(raw: String?): Map<String, Any>? {
        if (raw == null) return null
        return runCatching { objectToMap(JSONObject(raw)) }.getOrNull()
    }

    private fun objectToMap(json: JSONObject): Map<String, Any> =
        json.keys().asSequence().associateWith { toPlain(json.get(it)) }

    private fun toPlain(value: Any): Any = when (value) {
        is JSONObject -> objectToMap(value)
        is JSONArray -> (0 until value.length()).map { toPlain(value.get(it)) }
        else -> value
    }
}
