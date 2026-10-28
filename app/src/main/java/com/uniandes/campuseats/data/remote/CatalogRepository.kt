package com.uniandes.campuseats.data.remote

import android.content.Context
import androidx.core.content.edit
import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.CatalogOption
import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONArray
import org.json.JSONObject

interface CatalogCache {
    fun load(): String?
    fun save(raw: String)
}

class SharedPrefsCatalogCache(context: Context) : CatalogCache {
    private val prefs = context.getSharedPreferences("campus_eats_catalog", Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY, null)

    override fun save(raw: String) = prefs.edit { putString(KEY, raw) }

    private companion object {
        const val KEY = "catalog_json"
    }
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

    fun cached(): Catalog = runCatching { cache.load()?.let { parseCatalog(JSONObject(it)) } }
        .getOrNull() ?: Catalog.Empty

    suspend fun fetch(): Catalog {
        val json = api.get("/catalog")
        val parsed = parseCatalog(json)
        if (!parsed.isEmpty) cache.save(json.toString())
        return parsed
    }
}
