package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.CatalogOption
import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.cache.DocumentCache
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.textOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun extraOf(json: JSONObject?): Map<String, Any> {
    if (json == null) return emptyMap()
    return json.keys().asSequence().associateWith { key ->
        when (val value = json.get(key)) {
            is JSONArray -> (0 until value.length()).map { value.get(it).toString() }
            else -> value
        }
    }
}

fun parseCatalog(json: JSONObject): Catalog {
    val groups = json.optJSONObject("groups") ?: return Catalog.Empty
    return Catalog(
        groups.keys().asSequence().associateWith { key ->
            val rows = groups.getJSONArray(key)
            (0 until rows.length()).map {
                val row = rows.getJSONObject(it)
                CatalogOption(
                    value = row.getString("value"),
                    label = row.getString("label"),
                    detail = row.textOrNull("detail"),
                    emoji = row.textOrNull("emoji"),
                    extra = extraOf(row.optJSONObject("extra"))
                )
            }
        }
    )
}

class CatalogRepository(
    private val api: RemoteApi,
    private val cache: DocumentCache,
    private val policy: CachePolicy = CachePolicy()
) {

    suspend fun cached(): Catalog {
        val raw = attemptOrNull { cache.load(CacheKeys.CATALOG) } ?: return Catalog.Empty
        val parsed = attemptOrNull { parseCatalog(JSONObject(raw)) }
        if (parsed == null) {
            attemptOrNull { cache.clear(CacheKeys.CATALOG) }
            return Catalog.Empty
        }
        return parsed
    }

    suspend fun isStale(): Boolean = !policy.isFreshOrFalse(cache, CacheKeys.CATALOG, CacheTtl.CATALOG)

    suspend fun revalidate(force: Boolean = false): Catalog? =
        if (!force && !isStale()) null else fetch()

    suspend fun fetch(): Catalog {
        val json = api.get("/catalog")
        val parsed = parseCatalog(json)
        if (!parsed.isEmpty) cache.save(CacheKeys.CATALOG, json.toString(), policy.now())
        return parsed
    }
}
