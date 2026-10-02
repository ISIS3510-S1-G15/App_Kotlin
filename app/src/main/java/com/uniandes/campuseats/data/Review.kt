package com.uniandes.campuseats.data

data class ReviewRatings(
    val taste: Int,
    val attention: Int,
    val price: Int,
    val options: Int
) {
    val overall: Double get() = (taste + attention + price + options) / 4.0
}

data class Review(
    val id: String,
    val restaurantId: String,
    val author: String,
    val date: String, // yyyy-MM-dd
    val ratings: ReviewRatings,
    val dietaryOptions: List<String>,
    val comment: String
)

val seedReviews = listOf(
    Review(
        id = "r1",
        restaurantId = "1",
        author = "Mariana O.",
        date = "2026-09-08",
        ratings = ReviewRatings(taste = 4, attention = 5, price = 3, options = 4),
        dietaryOptions = listOf("Vegan options", "Gluten-free"),
        comment = "Great place to study and grab a coffee. The oat latte is amazing. A bit pricey but worth it for the atmosphere."
    ),
    Review(
        id = "r2",
        restaurantId = "1",
        author = "Sebastián R.",
        date = "2026-09-05",
        ratings = ReviewRatings(taste = 5, attention = 4, price = 3, options = 3),
        dietaryOptions = listOf("Vegetarian options"),
        comment = "Best coffee on campus by far. The almond croissant is incredible — always fresh."
    ),
    Review(
        id = "r3",
        restaurantId = "2",
        author = "Valeria M.",
        date = "2026-09-07",
        ratings = ReviewRatings(taste = 5, attention = 4, price = 4, options = 3),
        dietaryOptions = listOf("Vegan options"),
        comment = "The tonkotsu ramen is absolutely worth the wait. Rich broth, perfectly cooked egg. 10/10 would recommend to every student."
    ),
    Review(
        id = "r4",
        restaurantId = "3",
        author = "Andrés P.",
        date = "2026-09-03",
        ratings = ReviewRatings(taste = 4, attention = 3, price = 4, options = 2),
        dietaryOptions = emptyList(),
        comment = "The smash burger is legit. Wish they had more vegetarian options though. Service can be slow during rush hour."
    ),
    Review(
        id = "r5",
        restaurantId = "4",
        author = "Daniela C.",
        date = "2026-09-09",
        ratings = ReviewRatings(taste = 5, attention = 5, price = 4, options = 5),
        dietaryOptions = listOf("Vegan options", "Vegetarian options", "Gluten-free", "Nut-free"),
        comment = "Cosechas is my go-to after the gym. The green machine smoothie is so refreshing. They have great options for every diet!"
    )
)
