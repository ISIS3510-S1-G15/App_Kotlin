package com.uniandes.campuseats.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.ReviewRatings
import org.json.JSONArray
import org.json.JSONObject

class LegacyPreferencesMigration(private val context: Context, private val store: LocalStore) {

    private val marker = context.getSharedPreferences(MARKER_PREFS, Context.MODE_PRIVATE)

    suspend fun runOnce() {
        if (marker.getBoolean(KEY_DONE, false)) return
        val data = context.getSharedPreferences(DATA_PREFS, Context.MODE_PRIVATE)
        val profile = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
        val restaurants = context.getSharedPreferences(RESTAURANT_PREFS, Context.MODE_PRIVATE)
        val catalog = context.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE)

        reviewsFrom(data.getString("reviews", null)).takeIf { it.isNotEmpty() }?.let { store.saveReviews(it) }
        crowdingFrom(data.getString("crowding", null)).takeIf { it.isNotEmpty() }?.let { store.saveCrowding(it) }
        data.getStringSet("saved_ids", null)?.let { store.saveSavedIds(it.toSet()) }
        data.getString("recent_searches", null)?.let { store.saveRecentSearches(StringListCodec.decode(it)) }
        if (profile.getBoolean("survey_done", false)) store.saveProfile(profileFrom(profile), pendingSync = false)
        restaurants.getString("restaurants_json", null)?.let { store.saveDocument(CacheKeys.RESTAURANTS, it, 0L) }
        catalog.getString("catalog_json", null)?.let { store.saveDocument(CacheKeys.CATALOG, it, 0L) }

        listOf(data, profile, restaurants, catalog).forEach { prefs -> prefs.edit { clear() } }
        marker.edit { putBoolean(KEY_DONE, true) }
    }

    private fun reviewsFrom(raw: String?): List<Review> {
        if (raw == null) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { reviewFrom(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun reviewFrom(obj: JSONObject): Review {
        val diets = obj.getJSONArray("dietaryOptions")
        return Review(
            id = obj.getString("id"),
            restaurantId = obj.getString("restaurantId"),
            author = obj.getString("author"),
            date = obj.getString("date"),
            ratings = ReviewRatings(
                taste = obj.getInt("taste"),
                attention = obj.getInt("attention"),
                price = obj.getInt("price"),
                options = obj.getInt("options")
            ),
            dietaryOptions = (0 until diets.length()).map { diets.getString(it) },
            comment = obj.getString("comment")
        )
    }

    private fun crowdingFrom(raw: String?): Map<String, List<Int>> {
        if (raw == null) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { id ->
                val reports = obj.getJSONArray(id)
                (0 until reports.length()).map { reports.getInt(it) }
            }
        }.getOrDefault(emptyMap())
    }

    private fun profileFrom(prefs: SharedPreferences): Profile {
        fun list(key: String) = prefs.getString(key, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
        return Profile(
            name = prefs.getString("name", "").orEmpty(),
            frequency = prefs.getString("frequency", "").orEmpty(),
            budget = prefs.getString("budget", "").orEmpty(),
            dietaryRestrictions = list("dietary"),
            cuisinePreferences = list("cuisines"),
            usualMealTimes = list("meal_times"),
            topPriorities = list("priorities"),
            foodsToAvoid = list("avoid")
        )
    }

    private companion object {
        const val SEPARATOR = "|"
        const val MARKER_PREFS = "campus_eats_local_migration"
        const val KEY_DONE = "legacy_prefs_migrated"
        const val DATA_PREFS = "campus_eats_data"
        const val PROFILE_PREFS = "campus_eats_profile"
        const val RESTAURANT_PREFS = "campus_eats_restaurants"
        const val CATALOG_PREFS = "campus_eats_catalog"
    }
}
