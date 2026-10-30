package com.uniandes.campuseats.data.local

import android.content.Context
import androidx.core.content.edit
import com.uniandes.campuseats.ui.HomeFilters

class HomeFilterStore(context: Context) {

    data class Saved(val category: String, val filters: HomeFilters)

    private val prefs = context.applicationContext
        .getSharedPreferences("campus_eats_home_filters", Context.MODE_PRIVATE)

    fun load(): Saved = Saved(
        category = prefs.getString(KEY_CATEGORY, null) ?: DEFAULT_CATEGORY,
        filters = HomeFilters(
            openNow = prefs.getBoolean(KEY_OPEN_NOW, false),
            savedOnly = prefs.getBoolean(KEY_SAVED_ONLY, false),
            prices = prefs.getStringSet(KEY_PRICES, emptySet()).orEmpty().toSet(),
            minRating = prefs.getString(KEY_MIN_RATING, null)?.toDoubleOrNull() ?: 0.0
        )
    )

    fun save(category: String, filters: HomeFilters) {
        prefs.edit { 
            putString(KEY_CATEGORY, category)
            putBoolean(KEY_OPEN_NOW, filters.openNow)
            putBoolean(KEY_SAVED_ONLY, filters.savedOnly)
            putStringSet(KEY_PRICES, filters.prices.toSet())
            putString(KEY_MIN_RATING, filters.minRating.toString())
        }
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val DEFAULT_CATEGORY = "All"
        const val KEY_CATEGORY = "category"
        const val KEY_OPEN_NOW = "open_now"
        const val KEY_SAVED_ONLY = "saved_only"
        const val KEY_PRICES = "prices"
        const val KEY_MIN_RATING = "min_rating"
    }
}