package com.uniandes.campuseats.data

data class MenuItem(val name: String, val price: String, val description: String)

data class MenuSection(val category: String, val items: List<MenuItem>)

data class Restaurant(
    val id: String,
    val name: String,
    val location: String,
    val category: String,
    val image: Any, // drawable (Int) o URL (String); AsyncImage acepta ambos
    val rating: Double,
    val reviews: Int,
    val price: String,
    val waitTime: String,
    val tags: List<String>,
    val hours: String,
    val description: String,
    val mapX: Float, // porcentaje 0-100 del ancho del mapa
    val mapY: Float, // porcentaje 0-100 del alto del mapa
    val latitude: Double,
    val longitude: Double,
    val menu: List<MenuSection>
) {
    /** Context-aware: abierto o cerrado según la hora actual del teléfono y el horario del lugar. */
    val isOpen: Boolean get() = isOpenNow(hours)
}
