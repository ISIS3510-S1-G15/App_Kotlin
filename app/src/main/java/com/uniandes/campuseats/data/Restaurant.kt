package com.uniandes.campuseats.data

import com.uniandes.campuseats.R

data class MenuItem(val name: String, val price: String, val description: String)

data class MenuSection(val category: String, val items: List<MenuItem>)

data class Restaurant(
    val id: String,
    val name: String,
    val location: String,
    val category: String,
    val image: Any, // drawable (Int) o URL (String); AsyncImage acepta ambos
    val isOpen: Boolean,
    val rating: Double,
    val reviews: Int,
    val price: String,
    val waitTime: String,
    val tags: List<String>,
    val saved: Boolean, // solo el valor inicial; el estado real vive en AppDataStore
    val hours: String,
    val description: String,
    val mapX: Float, // porcentaje 0-100 del ancho del mapa
    val mapY: Float, // porcentaje 0-100 del alto del mapa
    val menu: List<MenuSection>
)

val restaurants = listOf(
    Restaurant(
        id = "1",
        name = "Starbucks",
        location = "Plazoleta Lleras",
        category = "Café",
        image = R.drawable.starbucks,
        isOpen = true,
        rating = 4.7,
        reviews = 1204,
        price = "$",
        waitTime = "2–5 min",
        tags = listOf("Coffee", "Pastries", "Study-friendly"),
        saved = false,
        hours = "6:30 AM – 11:00 PM",
        description = "The campus favorite for specialty coffee, house-made pastries, and a cozy atmosphere perfect for studying or catching up with friends.",
        mapX = 55f,
        mapY = 25f,
        menu = listOf(
            MenuSection(
                "Drinks",
                listOf(
                    MenuItem("Oat Latte", "$15.250", "Double shot, oat milk, light foam"),
                    MenuItem("Matcha Latte", "$15.750", "Ceremonial grade, choice of milk"),
                    MenuItem("Cold Brew", "$14.500", "18-hour steep, served over ice")
                )
            ),
            MenuSection(
                "Food",
                listOf(
                    MenuItem("Almond Croissant", "$8.000", "Twice-baked, almond cream, toasted flakes"),
                    MenuItem("Egg & Cheese Sandwich", "$12.500", "Everything bagel, scrambled egg, cheddar")
                )
            )
        )
    ),
    Restaurant(
        id = "2",
        name = "Kai Sushi",
        location = "Calle 20",
        category = "Asian",
        image = R.drawable.sushi,
        isOpen = true,
        rating = 4.5,
        reviews = 673,
        price = "$$",
        waitTime = "10–15 min",
        tags = listOf("Ramen", "Pho", "Noodles"),
        saved = false,
        hours = "11:00 AM – 9:00 PM",
        description = "Authentic ramen, pho, and pan-Asian noodle dishes made fresh daily. Student favorite for a warm, satisfying meal between classes.",
        mapX = 70f,
        mapY = 55f,
        menu = listOf(
            MenuSection(
                "Ramen",
                listOf(
                    MenuItem("Tonkotsu Ramen", "$20.500", "Rich pork broth, chashu, soft egg, nori"),
                    MenuItem("Spicy Miso Ramen", "$30.000", "Miso broth, tofu, corn, bamboo shoots")
                )
            ),
            MenuSection(
                "Pho",
                listOf(
                    MenuItem("Beef Pho", "$22.000", "12-hour broth, rare beef, herbs, bean sprouts"),
                    MenuItem("Veggie Pho", "$15.000", "Clear broth, tofu, mixed mushrooms")
                )
            )
        )
    ),
    Restaurant(
        id = "3",
        name = "La Puerta",
        location = "Calle del SD",
        category = "Burgers",
        image = R.drawable.la_puerta,
        isOpen = false,
        rating = 4.1,
        reviews = 528,
        price = "$$",
        waitTime = "8–12 min",
        tags = listOf("Burgers", "Fries", "Breakfast"),
        saved = true,
        hours = "11:00 AM – 8:00 PM",
        description = "Classic American grill serving smash burgers, crinkle fries, and tasty breakfast. Popular post-game spot for the athletics crowd.",
        mapX = 20f,
        mapY = 70f,
        menu = listOf(
            MenuSection(
                "Burgers",
                listOf(
                    MenuItem("Classic Smash", "$21.500", "Double smash patty, American cheese, pickles, special sauce"),
                    MenuItem("Mushroom Swiss", "$21.500", "Smash patty, sautéed mushrooms, Swiss cheese, truffle aioli"),
                    MenuItem("Veggie Burger", "$20.000", "House-made black bean patty, avocado, sprouts")
                )
            )
        )
    ),
    Restaurant(
        id = "4",
        name = "Cosechas",
        location = "Edificio ML",
        category = "Smoothies",
        image = "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=800&h=500&fit=crop&auto=format",
        isOpen = true,
        rating = 4.4,
        reviews = 287,
        price = "$$",
        waitTime = "3–7 min",
        tags = listOf("Smoothies", "Acai", "Healthy"),
        saved = false,
        hours = "6:30 AM – 8:00 PM",
        description = "Cold-pressed juices, smoothie bowls, and protein shakes designed for active students. Everything is made to order.",
        mapX = 60f,
        mapY = 35f,
        menu = listOf(
            MenuSection(
                "Smoothies",
                listOf(
                    MenuItem("Green Machine", "$8.500", "Spinach, banana, mango, ginger, coconut water"),
                    MenuItem("Berry Boost", "$8.500", "Mixed berries, açaí, almond butter, oat milk")
                )
            )
        )
    )
)
