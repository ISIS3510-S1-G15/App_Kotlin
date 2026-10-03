package com.uniandes.campuseats.data

import java.util.Locale

/** Un restaurante recomendado, su puntaje y las razones que se muestran al usuario. */
data class Recommendation(
    val restaurant: Restaurant,
    val score: Double,
    val reasons: List<String>
)

// Peso base de cada criterio; las prioridades de la encuesta duplican el criterio relacionado.
private const val W_CUISINE = 2.0
private const val W_DIET = 2.0
private const val W_BUDGET = 1.0
private const val W_RATING = 1.0
private const val W_CROWD = 1.0
private const val W_DISTANCE = 1.0
private const val PRIORITY_BOOST = 2.0
private const val AVOID_PENALTY = 2.0
private const val NEARBY_FULL_SCORE_M = 100f
private const val NEARBY_ZERO_SCORE_M = 1000f

/**
 * Smart feature: puntúa cada restaurante según el perfil de la encuesta, las reseñas de la
 * comunidad, la ocupación reportada, la distancia al usuario y la hora (solo abiertos).
 *
 * @param distances distancia en metros por id de restaurante (vacío si no hay ubicación).
 * @param nowMinutes hora del día en minutos desde medianoche (por defecto, la del teléfono).
 * @return los restaurantes ordenados de mejor a peor recomendación.
 */
fun recommend(
    restaurants: List<Restaurant>,
    profile: Profile,
    reviews: List<Review>,
    crowding: Map<String, List<Int>>,
    distances: Map<String, Float>,
    nowMinutes: Int = currentMinutesOfDay()
): List<Recommendation> {
    val open = restaurants.filter { isOpenAt(it.hours, nowMinutes) }
    val candidates = open.ifEmpty { restaurants } // si todo está cerrado, igual se recomienda algo

    val needs = profile.dietaryRestrictions.filter { it != "None" }
    val maxBudget = budgetRange(profile.budget)?.second
    val boost = { priority: String -> if (priority in profile.topPriorities) PRIORITY_BOOST else 1.0 }

    return candidates.map { r ->
        var score = 0.0
        val reasons = mutableListOf<String>()
        val restaurantReviews = reviews.filter { it.restaurantId == r.id }

        // Solo se llega aquí con lugares cerrados si TODO está cerrado: se avisa en la razón
        if (!isOpenAt(r.hours, nowMinutes)) reasons += "Closed right now"

        // Gustos: la categoría o las etiquetas coinciden con las cocinas favoritas
        val matchedCuisine = profile.cuisinePreferences.firstOrNull { pref ->
            cuisinesOf(r).any { it.equals(pref, ignoreCase = true) }
        }
        if (matchedCuisine != null) {
            score += W_CUISINE
            reasons += "Matches your taste for $matchedCuisine"
        }

        // Dieta: las reseñas de la comunidad dicen qué opciones ofrece
        if (needs.isNotEmpty() && restaurantReviews.isNotEmpty()) {
            val offered = restaurantReviews.flatMap { it.dietaryOptions }.toSet()
            val supported = needs.filter { dietLabel(it) in offered }
            score += W_DIET * (supported.size.toDouble() / needs.size)
            if (supported.size < needs.size) score -= W_DIET / 2
            if (supported.isNotEmpty()) reasons += "Offers ${supported.joinToString(" & ") { dietLabel(it) }}"
        }

        // Presupuesto: precio promedio del menú frente al máximo del usuario
        val avgPrice = averageItemPrice(r)
        if (maxBudget != null && avgPrice != null) {
            if (avgPrice <= maxBudget) {
                score += W_BUDGET * boost("Price")
                reasons += "Within your budget"
            } else {
                score -= W_BUDGET * 0.5 * boost("Price")
            }
        }

        // Calidad: promedio de reseñas (o calificación base si todavía no hay reseñas)
        val rating = if (restaurantReviews.isNotEmpty()) restaurantReviews.map { it.ratings.overall }.average() else r.rating
        score += W_RATING * ((rating - 3.0) / 2.0).coerceIn(0.0, 1.0) * boost("Taste")
        if (rating >= 4.5) reasons += "Rated ${String.format(Locale.US, "%.1f", rating)}★"

        // Ocupación: menos gente es mejor, sobre todo si el usuario prioriza la rapidez
        avgCrowding(crowding[r.id].orEmpty())?.let { crowd ->
            score += W_CROWD * ((5.0 - crowd) / 4.0).coerceIn(0.0, 1.0) * boost("Speed")
            if (crowd < 2.5) reasons += "${getCrowdingLevel(crowd).label} right now"
        }

        // Cercanía (solo si el GPS entrega la ubicación)
        distances[r.id]?.let { d ->
            val closeness = ((NEARBY_ZERO_SCORE_M - d) / (NEARBY_ZERO_SCORE_M - NEARBY_FULL_SCORE_M)).coerceIn(0f, 1f)
            score += W_DISTANCE * closeness * boost("Proximity")
            if (d <= 300f) reasons += "Only ${d.toInt()} m away"
        }

        // Comidas que el usuario quiere evitar
        if (profile.foodsToAvoid.any { avoids(r, it) }) {
            score -= AVOID_PENALTY
        }

        Recommendation(r, score, reasons)
    }.sortedByDescending { it.score }
}

private fun cuisinesOf(r: Restaurant): Set<String> = buildSet {
    add(r.category)
    addAll(r.tags)
    when (r.category) {
        "Burgers" -> add("American")
        "Smoothies" -> {
            add("Vegetarian")
            add("Vegan")
        }
    }
}

/** Etiqueta con la que las reseñas anotan cada restricción dietaria. */
private fun dietLabel(restriction: String): String = when (restriction) {
    "Vegetarian" -> "Vegetarian options"
    "Vegan" -> "Vegan options"
    else -> restriction
}

private fun avoids(r: Restaurant, food: String): Boolean {
    val term = food.trim().lowercase()
    if (term.isEmpty()) return false
    return r.menu.flatMap { it.items }.any {
        it.name.lowercase().contains(term) || it.description.lowercase().contains(term)
    } || r.tags.any { it.lowercase().contains(term) }
}

private val PriceRegex = Regex("""\$\s*(\d{1,3}(?:\.\d{3})*)""")

private fun parsePesos(text: String): Int? = PriceRegex.find(text)?.groupValues?.get(1)?.replace(".", "")?.toIntOrNull()

/** Precio promedio de los platos del menú, en pesos. */
private fun averageItemPrice(r: Restaurant): Double? =
    r.menu.flatMap { it.items }.mapNotNull { parsePesos(it.price) }.average().takeIf { !it.isNaN() }

/** (mínimo, máximo) en pesos de textos como "Under $8.000", "$8.000 – $15.000" u "Over $25.000". */
private fun budgetRange(budget: String): Pair<Int, Int>? {
    val numbers = PriceRegex.findAll(budget).mapNotNull { it.groupValues[1].replace(".", "").toIntOrNull() }.toList()
    return when {
        numbers.isEmpty() -> null
        budget.startsWith("Under", ignoreCase = true) -> 0 to numbers[0]
        budget.startsWith("Over", ignoreCase = true) -> numbers[0] to Int.MAX_VALUE
        numbers.size >= 2 -> numbers[0] to numbers[1]
        else -> 0 to numbers[0]
    }
}
