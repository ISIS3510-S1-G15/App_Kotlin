package com.uniandes.campuseats.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persiste en SharedPreferences los datos que el usuario genera en la app:
 * reseñas, reportes de ocupación y restaurantes guardados.
 * Si todavía no hay nada guardado se usan los datos semilla del prototipo.
 */
class AppDataStore(context: Context) {

    private val prefs = context.getSharedPreferences("campus_eats_data", Context.MODE_PRIVATE)

    // Reseñas

    fun loadReviews(): List<Review> {
        val json = prefs.getString(KEY_REVIEWS, null) ?: return seedReviews
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { reviewFromJson(array.getJSONObject(it)) }
        }.getOrDefault(seedReviews)
    }

    fun saveReviews(reviews: List<Review>) {
        val array = JSONArray()
        reviews.forEach { array.put(reviewToJson(it)) }
        prefs.edit { putString(KEY_REVIEWS, array.toString()) }
    }

    // Ocupación (crowding)

    fun loadCrowding(): Map<String, List<Int>> {
        val json = prefs.getString(KEY_CROWDING, null) ?: return seedCrowding
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { id ->
                val reports = obj.getJSONArray(id)
                (0 until reports.length()).map { reports.getInt(it) }
            }
        }.getOrDefault(seedCrowding)
    }

    fun saveCrowding(crowding: Map<String, List<Int>>) {
        val obj = JSONObject()
        crowding.forEach { (id, reports) -> obj.put(id, JSONArray(reports)) }
        prefs.edit { putString(KEY_CROWDING, obj.toString()) }
    }

    // Guardados

    fun loadSavedIds(): Set<String> =
        prefs.getStringSet(KEY_SAVED, null)?.toSet()
            ?: restaurants.filter { it.saved }.map { it.id }.toSet()

    fun saveSavedIds(ids: Set<String>) {
        prefs.edit { putStringSet(KEY_SAVED, ids) }
    }

    private fun reviewToJson(review: Review) = JSONObject().apply {
        put("id", review.id)
        put("restaurantId", review.restaurantId)
        put("author", review.author)
        put("date", review.date)
        put("taste", review.ratings.taste)
        put("attention", review.ratings.attention)
        put("price", review.ratings.price)
        put("options", review.ratings.options)
        put("dietaryOptions", JSONArray(review.dietaryOptions))
        put("comment", review.comment)
    }

    private fun reviewFromJson(obj: JSONObject): Review {
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

    private companion object {
        const val KEY_REVIEWS = "reviews"
        const val KEY_CROWDING = "crowding"
        const val KEY_SAVED = "saved_ids"
    }
}
