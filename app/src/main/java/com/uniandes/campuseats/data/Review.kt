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
