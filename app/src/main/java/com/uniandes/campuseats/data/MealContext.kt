package com.uniandes.campuseats.data

private const val WALKING_METERS_PER_MINUTE = 80f

const val CAMPUS_LATITUDE = 4.6018
const val CAMPUS_LONGITUDE = -74.0661
const val CAMPUS_RADIUS_METERS = 600f

val MealSlots = listOf("Breakfast", "Mid-morning snack", "Lunch", "Afternoon snack", "Dinner", "Late night")

fun mealSlotFor(hour: Int): String = when (hour) {
    in 5..8 -> "Breakfast"
    in 9..10 -> "Mid-morning snack"
    in 11..14 -> "Lunch"
    in 15..17 -> "Afternoon snack"
    in 18..21 -> "Dinner"
    else -> "Late night"
}

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

enum class MealMoment(val label: String, val categories: Set<String>) {
    Breakfast("Breakfast", setOf("Café", "Smoothies", "Dining Hall")),
    Lunch("Lunch", setOf("Dining Hall", "Asian", "Burgers", "Indian")),
    Snack("Snack", setOf("Café", "Smoothies")),
    Dinner("Dinner", setOf("Asian", "Burgers", "Indian", "Dining Hall"));

    fun fits(category: String): Boolean = category in categories

    companion object {
        fun at(hour: Int): MealMoment? = when (hour) {
            in 6..10 -> Breakfast
            in 11..14 -> Lunch
            in 15..17 -> Snack
            in 18..21 -> Dinner
            else -> null
        }
    }
}

fun walkingMinutes(meters: Float): Int = Math.ceil((meters / WALKING_METERS_PER_MINUTE).toDouble()).toInt().coerceIn(1, 999)

fun isOnCampus(distanceToCampusMeters: Float): Boolean = distanceToCampusMeters <= CAMPUS_RADIUS_METERS

data class SpotSuggestion(val restaurant: Restaurant, val meal: MealMoment?, val distanceMeters: Float)

fun closestOpenSpot(
    restaurants: List<Restaurant>,
    distances: Map<String, Float>,
    hour: Int,
    minutesOfDay: Int
): SpotSuggestion? {
    val open = restaurants
        .filter { it.id in distances && isOpenAt(it.hours, minutesOfDay) }
        .sortedBy { distances.getValue(it.id) }
    if (open.isEmpty()) return null
    val meal = MealMoment.at(hour)
    val pick = meal?.let { m -> open.firstOrNull { m.fits(it.category) } }
    return SpotSuggestion(pick ?: open.first(), if (pick != null) meal else null, distances.getValue((pick ?: open.first()).id))
}
