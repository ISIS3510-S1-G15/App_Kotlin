package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.CatalogOption
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
import org.json.JSONArray
import org.json.JSONObject

interface CatalogCache {
    suspend fun load(): String?
    suspend fun save(raw: String)
}

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

class CatalogRepository(private val api: RemoteApi, private val cache: CatalogCache) {

    suspend fun cached(): Catalog = attemptOrNull { cache.load()?.let { parseCatalog(JSONObject(it)) } }
        ?: Catalog.Empty

    suspend fun fetch(): Catalog {
        val json = api.get("/catalog")
        val parsed = parseCatalog(json)
        if (!parsed.isEmpty) cache.save(json.toString())
        return parsed
    }
}
