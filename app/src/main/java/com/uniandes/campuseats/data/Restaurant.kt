package com.uniandes.campuseats.data

import com.uniandes.campuseats.R

data class MenuItem(val name: String, val price: String, val description: String)

data class Restaurant(
    val id: String,
    val name: String,
    val location: String,
    val category: String,
    val image: Int,
    val isOpen: Boolean,
    val rating: Double,
    val reviews: Int,
    val price: String,
    val waitTime: String,
    val tags: List<String>,
    val saved: Boolean,
    val hours: String,
    val description: String,
    val mapX: Float,
    val mapY: Float,
    val menu: List<MenuItem>
)

val restaurants = listOf(
    Restaurant("1", "Starbucks", "Plazoleta Lleras", "Cafe", R.drawable.starbucks, true, 4.7, 1204, "$", "2-5 min", listOf("Coffee", "Pastries"), true, "6:30am - 8:00pm", "The campus favorite for specialty coffee, house-made pastries, and a cozy atmosphere perfect for studying.", .55f, .25f, listOf(MenuItem("Oat Latte", "$15.250", "Double shot, oat milk, light foam"), MenuItem("Matcha Latte", "$15.750", "Ceremonial grade, choice of milk"))),
    Restaurant("2", "Yamato Sushi Wok", "Calle 20", "Asian", R.drawable.sushi, true, 4.5, 673, "$$", "10-15 min", listOf("Ramen", "Pho"), false, "11:30am - 9:00pm", "Authentic ramen, pho, and pan-Asian noodle dishes made fresh daily.", .70f, .55f, listOf(MenuItem("Tonkotsu Ramen", "$20.500", "Rich pork broth, chashu, soft egg, nori"), MenuItem("Beef Pho", "$22.000", "12-hour broth, rare beef, herbs, bean sprouts"))),
    Restaurant("3", "Cosechas", "Calle 18", "Smoothies", R.drawable.starbucks, true, 4.4, 210, "$", "5 min", listOf("Vegan options", "Smoothies"), true, "7:00am - 5:00pm", "Cold-pressed juices, smoothie bowls, and protein shakes made to order.", .60f, .35f, listOf(MenuItem("Green Machine", "$8.500", "Spinach, banana, mango, ginger, coconut water"), MenuItem("Berry Boost", "$8.500", "Mixed berries, acai, almond butter, oat milk"))),
    Restaurant("4", "La Puerta", "Calle 19", "Dining Hall", R.drawable.la_puerta, false, 4.2, 340, "$", "5-10 min", listOf("Burgers"), true, "11:30am - 8:00pm", "Classic campus grill serving smash burgers, crinkle fries, and tasty breakfast.", .20f, .70f, listOf(MenuItem("Classic Smash", "$21.500", "Double smash patty, American cheese, pickles"), MenuItem("Veggie Burger", "$20.000", "House-made black bean patty, avocado, sprouts")))
)