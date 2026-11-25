package com.uniandes.campuseats.data.outbox

import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.IdempotencyKey
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class FlushResult(
    val sentKinds: Set<String>,
    val dropped: Int,
    val remaining: Int,
    /** El servidor respondió 401: la sesión expiró. La cola NO se borra; se retoma al volver a iniciar sesión. */
    val authExpired: Boolean = false
) {
    val sent: Boolean get() = sentKinds.isNotEmpty()
}

class OutboxFlusher(
    private val api: RemoteApi,
    private val store: OutboxStore,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Mutex()

    /**
     * Envía la cola. Cada categoría se envía en su propia corrutina (en orden dentro de la categoría).
     * Entre reintentos de un 5xx/408/429 se aplica backoff exponencial; [ignoreBackoff] lo salta
     * (botón "Sync now").
     */
    suspend fun flush(ignoreBackoff: Boolean = false): FlushResult = lock.withLock {
        val pending = store.pending()
        if (pending.isEmpty()) return FlushResult(emptySet(), 0, 0)

        val sentKinds = Collections.synchronizedSet(mutableSetOf<String>())
        val dropped = AtomicInteger(0)
        val authExpired = AtomicBoolean(false)

        coroutineScope {
            for ((kind, group) in pending.groupBy { it.request.kind }) {
                launch {
                    for (entry in group) {
                        // respetamos el orden de la categoría: si la primera está aplazada, el resto espera
                        if (!ignoreBackoff && entry.nextAttemptAt > clock()) break
                        when (deliver(entry.request)) {
                            Delivery.Sent -> {
                                store.remove(entry.id)
                                sentKinds.add(kind)
                            }
                            Delivery.Rejected -> {
                                store.remove(entry.id)
                                dropped.incrementAndGet()
                            }
                            Delivery.ServerError -> {
                                if (entry.attempts + 1 >= maxAttempts) {
                                    store.remove(entry.id)
                                    dropped.incrementAndGet()
                                } else {
                                    store.recordAttempt(entry.id)
                                    store.scheduleRetry(entry.id, clock() + backoffMs(entry.attempts + 1))
                                }
                                break // un error solo detiene la cola de esa categoría
                            }
                            Delivery.AuthExpired -> {
                                authExpired.set(true)
                                break
                            }
                            Delivery.Unreachable -> break
                        }
                    }
                }
            }
        }

        FlushResult(sentKinds.toSet(), dropped.get(), store.count(), authExpired.get())
    }

    private suspend fun deliver(request: OutboxRequest): Delivery = try {
        // Misma llave en cada reintento: si el servidor ya guardó la petición, no la duplica.
        withContext(IdempotencyKey(request.requestId)) {
            when (request.method) {
                "PUT" -> api.put(request.path, request.body)
                "DELETE" -> api.delete(request.path)
                else -> api.post(request.path, request.body ?: emptyMap())
            }
        }
        Delivery.Sent
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        when {
            e.statusCode == 401 -> Delivery.AuthExpired
            e.statusCode in 400..499 && e.statusCode != 408 && e.statusCode != 429 -> Delivery.Rejected
            else -> Delivery.ServerError
        }
    } catch (e: Exception) {
        Delivery.Unreachable
    }

    private enum class Delivery { Sent, Rejected, ServerError, Unreachable, AuthExpired }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
        private const val BASE_BACKOFF_MS = 30_000L
        private const val MAX_BACKOFF_MS = 15 * 60_000L

        fun backoffMs(attempt: Int): Long {
            val exponent = (attempt - 1).coerceIn(0, 10)
            return (BASE_BACKOFF_MS shl exponent).coerceAtMost(MAX_BACKOFF_MS)
        }
    }
}
