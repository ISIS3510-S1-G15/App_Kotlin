package com.uniandes.campuseats.data

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

class MealMoment(val label: String, val categories: Set<String>, val startHour: Int, val endHour: Int) {

    fun fits(category: String): Boolean = category in categories

    fun covers(hour: Int): Boolean = hour in startHour..endHour
}

data class SpotSuggestion(val restaurant: Restaurant, val meal: MealMoment?, val distanceMeters: Float)

class MealRules(catalog: Catalog) {

    private val slots = catalog.options("meal_slots")

    val moments: List<MealMoment> = catalog.options("meal_moments").mapNotNull { option ->
        val start = option.number("start_hour")?.toInt()
        val end = option.number("end_hour")?.toInt()
        if (start == null || end == null) null else MealMoment(option.label, option.strings("categories").toSet(), start, end)
    }

    val campusLatitude: Double? = catalog.number("campus_location", "latitude")
    val campusLongitude: Double? = catalog.number("campus_location", "longitude")
    private val campusRadiusMeters: Float? = catalog.number("campus_location", "radius_meters")?.toFloat()
    private val walkingMetersPerMinute: Float = catalog.number("campus_location", "walking_meters_per_minute")
        ?.toFloat()?.coerceAtLeast(1f) ?: 1f

    fun mealSlotFor(hour: Int): String {
        val match = slots.firstOrNull { option ->
            val start = option.number("start_hour")
            val end = option.number("end_hour")
            start != null && end != null && hour in start.toInt()..end.toInt()
        }
        return (match ?: slots.firstOrNull { it.extra["fallback"] == true })?.label.orEmpty()
    }

    fun momentAt(hour: Int): MealMoment? = moments.firstOrNull { it.covers(hour) }

    fun walkingMinutes(meters: Float): Int =
        Math.ceil((meters / walkingMetersPerMinute).toDouble()).toInt().coerceIn(1, 999)

    fun isOnCampus(distanceToCampusMeters: Float): Boolean =
        campusRadiusMeters != null && distanceToCampusMeters <= campusRadiusMeters

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
        val meal = momentAt(hour)
        val pick = meal?.let { m -> open.firstOrNull { m.fits(it.category) } }
        val chosen = pick ?: open.first()
        return SpotSuggestion(chosen, if (pick != null) meal else null, distances.getValue(chosen.id))
    }
}
