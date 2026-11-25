package com.uniandes.campuseats.data.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

data class CacheStats(
    val hits: Long = 0,
    val misses: Long = 0,
    val evictions: Long = 0,
    val expirations: Long = 0,
    val size: Int = 0,
    val maxEntries: Int = 0
) {
    val hitRate: Double get() = (hits + misses).let { if (it == 0L) 0.0 else hits.toDouble() / it }
}

/**
 * Cache en memoria (nivel L1) completamente independiente del almacenamiento local:
 *  - evicción LRU por número de entradas,
 *  - TTL por entrada,
 *  - de-duplicación de cargas en vuelo ([getOrLoad]): N llamadas concurrentes con la misma llave = 1 sola petición,
 *  - estadísticas observables ([stats]) para la pantalla de ajustes.
 *
 * Es thread-safe y no depende de Android, así que se puede probar en JVM.
 */
class MemoryCache<K : Any, V : Any>(
    maxEntries: Int,
    private val defaultTtlMs: Long = Long.MAX_VALUE,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Entry<V>(val value: V, val expiresAt: Long)

    private val lock = Any()
    private val map = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)
    private val inFlight = HashMap<K, CompletableDeferred<V>>()
    private var max = maxEntries.coerceAtLeast(1)
    private var hits = 0L
    private var misses = 0L
    private var evictions = 0L
    private var expirations = 0L

    private val _stats = MutableStateFlow(CacheStats(maxEntries = max))
    val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    val size: Int get() = synchronized(lock) { map.size }

    fun get(key: K): V? = synchronized(lock) {
        val entry = map[key]
        val value = when {
            entry == null -> null
            entry.expiresAt <= clock() -> {
                map.remove(key)
                expirations++
                null
            }
            else -> entry.value
        }
        if (value == null) misses++ else hits++
        publish()
        value
    }

    /** Lectura sin tocar estadísticas ni el orden LRU (para decidir si hay algo en memoria). */
    fun peek(key: K): V? = synchronized(lock) {
        map[key]?.takeIf { it.expiresAt > clock() }?.value
    }

    fun put(key: K, value: V, ttlMs: Long = defaultTtlMs) {
        synchronized(lock) {
            val expiresAt = clock().let { now -> if (ttlMs >= Long.MAX_VALUE - now) Long.MAX_VALUE else now + ttlMs }
            map[key] = Entry(value, expiresAt)
            trimLocked(max)
            publish()
        }
    }

    suspend fun getOrLoad(key: K, ttlMs: Long = defaultTtlMs, loader: suspend () -> V): V {
        get(key)?.let { return it }
        while (true) {
            var owner: CompletableDeferred<V>? = null
            val shared = synchronized(lock) {
                inFlight[key] ?: CompletableDeferred<V>().also {
                    inFlight[key] = it
                    owner = it
                }
            }
            val mine = owner
            if (mine == null) {
                try {
                    return shared.await()
                } catch (e: CancellationException) {
                    // si cancelaron al dueño pero yo sigo vivo, intento de nuevo siendo el dueño
                    if (!currentCoroutineContext().isActive) throw e
                    continue
                }
            }
            try {
                val value = loader()
                put(key, value, ttlMs)
                mine.complete(value)
                return value
            } catch (e: CancellationException) {
                mine.cancel(e)
                throw e
            } catch (e: Throwable) {
                mine.completeExceptionally(e)
                throw e
            } finally {
                synchronized(lock) { if (inFlight[key] === mine) inFlight.remove(key) }
            }
        }
    }

    fun invalidate(key: K) {
        synchronized(lock) {
            map.remove(key)
            publish()
        }
    }

    fun invalidateWhere(predicate: (K) -> Boolean) {
        synchronized(lock) {
            map.keys.removeAll(map.keys.filter(predicate).toSet())
            publish()
        }
    }

    fun clear() {
        synchronized(lock) {
            map.clear()
            publish()
        }
    }

    /** Cambia la capacidad máxima (p. ej. cuando el usuario ajusta el tamaño en preferencias). */
    fun resize(maxEntries: Int) {
        synchronized(lock) {
            max = maxEntries.coerceAtLeast(1)
            trimLocked(max)
            publish()
        }
    }

    /** Libera memoria cuando el sistema lo pide (onTrimMemory): deja solo [fraction] de la capacidad. */
    fun trimTo(fraction: Float) {
        synchronized(lock) {
            trimLocked((max * fraction.coerceIn(0f, 1f)).toInt())
            publish()
        }
    }

    private fun trimLocked(limit: Int) {
        if (map.size > limit) {
            val it = map.entries.iterator()
            while (map.size > limit && it.hasNext()) {
                it.next()
                it.remove()
                evictions++
            }
        }
    }

    private fun publish() {
        _stats.value = CacheStats(hits, misses, evictions, expirations, map.size, max)
    }
}
