package com.uniandes.campuseats.data.cache

class CachedDocument(val payload: String, val fetchedAt: Long)

/**
 * Cache de documentos de dos niveles: memoria (L1, [MemoryCache]) delante de disco (L2, Room).
 *  - Lectura: L1 -> L2 (y se promueve a L1) -> miss.
 *  - Escritura: siempre a L1; a L2 solo si [persistToDisk] (preferencia "guardar cache en disco").
 * Con [persistToDisk] = false la app funciona con cache puramente en memoria (se pierde al cerrar el proceso).
 */
class TieredDocumentCache(
    private val disk: DocumentCache,
    private val memory: MemoryCache<String, CachedDocument>,
    private val persistToDisk: () -> Boolean = { true }
) : DocumentCache {

    override suspend fun load(key: String): String? {
        memory.get(key)?.let { return it.payload }
        val payload = disk.load(key) ?: return null
        val fetchedAt = disk.fetchedAt(key) ?: return payload
        memory.put(key, CachedDocument(payload, fetchedAt))
        return payload
    }

    override suspend fun fetchedAt(key: String): Long? {
        memory.peek(key)?.let { return it.fetchedAt }
        return disk.fetchedAt(key)
    }

    override suspend fun save(key: String, raw: String, fetchedAt: Long) {
        memory.put(key, CachedDocument(raw, fetchedAt))
        if (persistToDisk()) disk.save(key, raw, fetchedAt)
    }

    override suspend fun clear(key: String) {
        memory.invalidate(key)
        disk.clear(key)
    }
}
