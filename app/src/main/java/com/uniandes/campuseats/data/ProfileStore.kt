package com.uniandes.campuseats.data

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda el perfil de la encuesta en SharedPreferences para que la encuesta
 * inicial solo aparezca la primera vez que se abre la app.
 * Las listas se guardan unidas con SEPARATOR para conservar el orden.
 */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("campus_eats_profile", Context.MODE_PRIVATE)

    val isSurveyDone: Boolean get() = prefs.getBoolean(KEY_SURVEY_DONE, false)

    fun load(): Profile = Profile(
        name = prefs.getString(KEY_NAME, "").orEmpty(),
        frequency = prefs.getString(KEY_FREQUENCY, "").orEmpty(),
        budget = prefs.getString(KEY_BUDGET, "").orEmpty(),
        dietaryRestrictions = getList(KEY_DIETARY),
        cuisinePreferences = getList(KEY_CUISINES),
        usualMealTimes = getList(KEY_MEAL_TIMES),
        topPriorities = getList(KEY_PRIORITIES),
        foodsToAvoid = getList(KEY_AVOID)
    )

    fun save(profile: Profile) {
        prefs.edit {
            putBoolean(KEY_SURVEY_DONE, true)
            putString(KEY_NAME, profile.name)
            putString(KEY_FREQUENCY, profile.frequency)
            putString(KEY_BUDGET, profile.budget)
            putString(KEY_DIETARY, profile.dietaryRestrictions.joinToString(SEPARATOR))
            putString(KEY_CUISINES, profile.cuisinePreferences.joinToString(SEPARATOR))
            putString(KEY_MEAL_TIMES, profile.usualMealTimes.joinToString(SEPARATOR))
            putString(KEY_PRIORITIES, profile.topPriorities.joinToString(SEPARATOR))
            putString(KEY_AVOID, profile.foodsToAvoid.joinToString(SEPARATOR))
        }
    }

    private fun getList(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }

    private companion object {
        const val SEPARATOR = "|"
        const val KEY_SURVEY_DONE = "survey_done"
        const val KEY_NAME = "name"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_BUDGET = "budget"
        const val KEY_DIETARY = "dietary"
        const val KEY_CUISINES = "cuisines"
        const val KEY_MEAL_TIMES = "meal_times"
        const val KEY_PRIORITIES = "priorities"
        const val KEY_AVOID = "avoid"
    }
}
