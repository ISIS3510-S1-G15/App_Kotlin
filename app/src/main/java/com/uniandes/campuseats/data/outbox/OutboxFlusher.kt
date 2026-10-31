package com.uniandes.campuseats.data.outbox

import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.RemoteApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

data class FlushResult(val sentKinds: Set<String>, val dropped: Int, val remaining: Int) {
    val sent: Boolean get() = sentKinds.isNotEmpty()
}

class OutboxFlusher(
    private val api: RemoteApi,
    private val store: OutboxStore,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS
) {
    private val lock = Mutex()

    suspend fun flush(): FlushResult = lock.withLock {
        val pending = store.pending()
        if (pending.isEmpty()) return FlushResult(emptySet(), 0, 0)

        val sentKinds = Collections.synchronizedSet(mutableSetOf<String>())
        var dropped = 0
        val dropLock = Mutex()

        coroutineScope {
            // Agrupamos las peticiones independientes por categoria
            val groupedEntries = pending.groupBy { it.request.kind }

            for ((kind, group) in groupedEntries) {
                // Lanzamos una corrutina en paralelo para cada categoria
                launch {
                    android.util.Log.d("TEST_OUTBOX", "Lanzando hilo paralelo para la categoria: $kind")
                    for (entry in group) {
                        when (deliver(entry.request)) {
                            Delivery.Sent -> {
                                store.remove(entry.id)
                                sentKinds.add(kind)
                            }
                            Delivery.Rejected -> {
                                store.remove(entry.id)
                                dropLock.withLock { dropped++ }
                            }
                            Delivery.ServerError -> {
                                if (entry.attempts + 1 >= maxAttempts) {
                                    store.remove(entry.id)
                                    dropLock.withLock { dropped++ }
                                } else {
                                    store.recordAttempt(entry.id)
                                }
                                // Si hay un error solo la cola de esa categoria se detiene
                                break
                            }
                            Delivery.Unreachable -> break
                        }
                    }
                }
            }
        }

        FlushResult(sentKinds.toSet(), dropped, store.count())
    }

    private suspend fun deliver(request: OutboxRequest): Delivery = try {
        when (request.method) {
            "PUT" -> api.put(request.path, request.body)
            "DELETE" -> api.delete(request.path)
            else -> api.post(request.path, request.body ?: emptyMap())
        }
        Delivery.Sent
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        when {
            e.statusCode == 401 -> Delivery.Unreachable
            e.statusCode in 400..499 && e.statusCode != 408 && e.statusCode != 429 -> Delivery.Rejected
            else -> Delivery.ServerError
        }
    } catch (e: Exception) {
        Delivery.Unreachable
    }

    private enum class Delivery { Sent, Rejected, ServerError, Unreachable }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5
    }
}