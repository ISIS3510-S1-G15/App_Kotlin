package com.uniandes.campuseats.data.remote

import android.content.Context
import androidx.core.content.edit
import com.uniandes.campuseats.R
import com.uniandes.campuseats.data.MenuItem
import com.uniandes.campuseats.data.MenuSection
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONObject

interface RestaurantCache {
    fun load(): String?
    fun save(raw: String)
}

class SharedPrefsRestaurantCache(context: Context) : RestaurantCache {
    private val prefs = context.getSharedPreferences("campus_eats_restaurants", Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY, null)

    override fun save(raw: String) = prefs.edit { putString(KEY, raw) }

    private companion object {
        const val KEY = "restaurants_json"
    }
}

fun bundledImageFor(key: String): Int? = when (key) {
    "starbucks" -> R.drawable.starbucks
    "sushi" -> R.drawable.sushi
    "la_puerta" -> R.drawable.la_puerta
    else -> null
}

fun parseRestaurants(json: JSONObject, imageFor: (String) -> Int? = ::bundledImageFor): List<Restaurant> {
    val rows = json.optJSONArray("restaurants") ?: return emptyList()
    return (0 until rows.length()).map { restaurantFromRemote(rows.getJSONObject(it), imageFor) }
}

private fun restaurantFromRemote(json: JSONObject, imageFor: (String) -> Int?): Restaurant {
    val bundled = json.optString("image_key").takeIf { it.isNotBlank() }?.let(imageFor)
    return Restaurant(
        id = json.getString("id"),
        name = json.getString("name"),
        location = json.getString("location"),
        category = json.getString("category"),
        image = bundled ?: json.optString("image_url"),
        rating = json.getDouble("rating"),
        reviews = json.getInt("review_count"),
        price = json.getString("price"),
        waitTime = json.getString("wait_time"),
        tags = json.optJSONArray("tags").toStringList(),
        hours = json.getString("hours"),
        description = json.getString("description"),
        mapX = json.getDouble("map_x").toFloat(),
        mapY = json.getDouble("map_y").toFloat(),
        latitude = json.getDouble("latitude"),
        longitude = json.getDouble("longitude"),
        menu = menuFromRemote(json)
    )
}

private fun menuFromRemote(json: JSONObject): List<MenuSection> {
    val sections = json.optJSONArray("menu") ?: return emptyList()
    return (0 until sections.length()).map { index ->
        val section = sections.getJSONObject(index)
        val items = section.optJSONArray("items")
        MenuSection(
            category = section.getString("category"),
            items = if (items == null) emptyList() else (0 until items.length()).map {
                val item = items.getJSONObject(it)
                MenuItem(item.getString("name"), item.getString("price"), item.getString("description"))
            }
        )
    }
}

class RestaurantRepository(
    private val api: RemoteApi,
    private val cache: RestaurantCache,
    private val imageFor: (String) -> Int? = ::bundledImageFor
) {

    fun cached(): List<Restaurant> = runCatching {
        cache.load()?.let { parseRestaurants(JSONObject(it), imageFor) }
    }.getOrNull().orEmpty()

    suspend fun fetch(): List<Restaurant> {
        val json = api.get("/restaurants")
        val parsed = parseRestaurants(json, imageFor)
        if (parsed.isNotEmpty()) cache.save(json.toString())
        return parsed
    }
}
