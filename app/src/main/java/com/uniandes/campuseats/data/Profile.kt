package com.uniandes.campuseats.data

data class Profile(
    val name: String,
    val frequency: String,
    val budget: String,
    val dietaryRestrictions: List<String>,
    val cuisinePreferences: List<String>,
    val usualMealTimes: List<String>,
    val topPriorities: List<String>,
    val foodsToAvoid: List<String>
)

val mockProfile = Profile(
    name = "Kevin",
    frequency = "Once a week",
    budget = "$8.000 - $15.000",
    dietaryRestrictions = listOf("Vegetarian", "Vegan"),
    cuisinePreferences = listOf("Colombian", "Asian"),
    usualMealTimes = listOf("Breakfast"),
    topPriorities = listOf("Price"),
    foodsToAvoid = listOf("Spicy")
)