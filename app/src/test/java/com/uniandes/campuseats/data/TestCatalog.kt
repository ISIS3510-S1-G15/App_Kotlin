package com.uniandes.campuseats.data

private fun option(value: String, extra: Map<String, Any>) = CatalogOption(value, value, null, null, extra)

val testCatalog = Catalog(
    mapOf(
        "meal_slots" to listOf(
            option("Breakfast", mapOf("start_hour" to 5, "end_hour" to 8)),
            option("Mid-morning snack", mapOf("start_hour" to 9, "end_hour" to 10)),
            option("Lunch", mapOf("start_hour" to 11, "end_hour" to 14)),
            option("Afternoon snack", mapOf("start_hour" to 15, "end_hour" to 17)),
            option("Dinner", mapOf("start_hour" to 18, "end_hour" to 21)),
            option("Late night", mapOf("fallback" to true))
        ),
        "meal_moments" to listOf(
            option("Breakfast", mapOf("start_hour" to 6, "end_hour" to 10, "categories" to listOf("Café", "Smoothies", "Dining Hall"))),
            option("Lunch", mapOf("start_hour" to 11, "end_hour" to 14, "categories" to listOf("Dining Hall", "Asian", "Burgers", "Indian"))),
            option("Snack", mapOf("start_hour" to 15, "end_hour" to 17, "categories" to listOf("Café", "Smoothies"))),
            option("Dinner", mapOf("start_hour" to 18, "end_hour" to 21, "categories" to listOf("Asian", "Burgers", "Indian", "Dining Hall")))
        ),
        "campus_location" to listOf(
            option("latitude", mapOf("number" to 4.6018)),
            option("longitude", mapOf("number" to -74.0661)),
            option("radius_meters", mapOf("number" to 600)),
            option("walking_meters_per_minute", mapOf("number" to 80))
        )
    )
)

val testMealRules = MealRules(testCatalog)
