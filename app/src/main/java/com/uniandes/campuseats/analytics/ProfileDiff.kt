package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.data.Profile

object ProfileDiff {
    const val DIETARY_RESTRICTIONS = "dietary_restrictions"
    const val CUISINE_PREFERENCES = "cuisine_preferences"

    fun changedFields(old: Profile, updated: Profile): List<String> = buildList {
        if (old.dietaryRestrictions.toSet() != updated.dietaryRestrictions.toSet()) add(DIETARY_RESTRICTIONS)
        if (old.cuisinePreferences.toSet() != updated.cuisinePreferences.toSet()) add(CUISINE_PREFERENCES)
    }
}
