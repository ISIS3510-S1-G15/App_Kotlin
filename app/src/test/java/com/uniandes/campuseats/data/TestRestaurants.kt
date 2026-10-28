package com.uniandes.campuseats.data

private fun spot(
    id: String,
    name: String,
    category: String,
    hours: String,
    price: String,
    rating: Double,
    tags: List<String> = listOf(category),
    menu: List<MenuSection> = emptyList()
) =
    Restaurant(
        id = id,
        name = name,
        location = "Campus",
        category = category,
        image = "",
        rating = rating,
        reviews = 100,
        price = price,
        waitTime = "5 min",
        tags = tags,
        hours = hours,
        description = "",
        mapX = 50f,
        mapY = 50f,
        latitude = 4.6,
        longitude = -74.06,
        menu = menu
    )

val restaurants = listOf(
    spot("1", "Starbucks", "Café", "6:30 AM – 11:00 PM", "$", 4.7, listOf("Coffee", "Pastries", "Study-friendly")),
    spot("2", "Kai Sushi", "Asian", "11:00 AM – 9:00 PM", "$$", 4.5, listOf("Ramen", "Pho", "Noodles")),
    spot(
        "3", "La Puerta", "Burgers", "11:00 AM – 8:00 PM", "$$", 4.1,
        listOf("Burgers", "Fries", "Breakfast"),
        listOf(MenuSection("Burgers", listOf(MenuItem("Mushroom Swiss", "$21.500", "Smash patty, sautéed mushrooms"))))
    ),
    spot("4", "Cosechas", "Smoothies", "6:30 AM – 8:00 PM", "$$", 4.4, listOf("Smoothies", "Acai", "Healthy"))
)
