package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONArray
import org.json.JSONObject

private const val MAX_PRIORITIES = 3
private const val MAX_NAME = 60
private const val MAX_DISLIKED = 300

fun Profile.toRemoteBody(): Map<String, Any> = mapOf(
    "name" to name.take(MAX_NAME),
    "cuisines" to cuisinePreferences,
    "dietary_restrictions" to dietaryRestrictions,
    "meal_frequency" to frequency,
    "meal_times" to usualMealTimes,
    "budget" to budget,
    "priorities" to topPriorities.take(MAX_PRIORITIES),
    "disliked_foods" to foodsToAvoid.joinToString(", ").take(MAX_DISLIKED)
)

fun profileFromRemote(json: JSONObject) = Profile(
    name = json.optString("name"),
    frequency = json.optString("meal_frequency"),
    budget = json.optString("budget"),
    dietaryRestrictions = json.optJSONArray("dietary_restrictions").toStringList(),
    cuisinePreferences = json.optJSONArray("cuisines").toStringList(),
    usualMealTimes = json.optJSONArray("meal_times").toStringList(),
    topPriorities = json.optJSONArray("priorities").toStringList(),
    foodsToAvoid = json.optString("disliked_foods").split(",").map { it.trim() }.filter { it.isNotEmpty() }
)

internal fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

class ProfileRepository(private val api: RemoteApi) {

    suspend fun fetch(): Profile? {
        val json = api.get("/me/profile")
        return if (json.optBoolean("completed")) profileFromRemote(json) else null
    }

    suspend fun save(profile: Profile) {
        api.put("/me/profile", profile.toRemoteBody())
    }
}
