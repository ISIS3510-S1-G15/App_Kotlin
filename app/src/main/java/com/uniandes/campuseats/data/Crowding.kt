package com.uniandes.campuseats.data

import androidx.compose.ui.graphics.Color

data class CrowdingLevel(
    val label: String,
    val color: Color,    // color del texto
    val bg: Color,       // fondo del badge
    val border: Color,   // borde del badge
    val barColor: Color  // relleno de las barras
)

fun getCrowdingLevel(avg: Double): CrowdingLevel = when {
    avg < 1.5 -> CrowdingLevel("Empty", Color(0xFF2D7A4F), Color(0xFFE3F5EC), Color(0xFFB6DEC8), Color(0xFF3A8C4F))
    avg < 2.5 -> CrowdingLevel("Quiet", Color(0xFF2563A8), Color(0xFFE3EDFA), Color(0xFFB6CFF0), Color(0xFF3B82C4))
    avg < 3.5 -> CrowdingLevel("Moderate", Color(0xFF9A6B00), Color(0xFFFEF3CD), Color(0xFFF5D98B), Color(0xFFF4A735))
    avg < 4.5 -> CrowdingLevel("Busy", Color(0xFFB94D00), Color(0xFFFDE8D8), Color(0xFFF5BF9A), Color(0xFFE8640A))
    else -> CrowdingLevel("Very Busy", Color(0xFFB91C1C), Color(0xFFFDE8E8), Color(0xFFF5AAAA), Color(0xFFDC2626))
}

fun avgCrowding(reports: List<Int>): Double? =
    if (reports.isEmpty()) null else reports.average()

val seedCrowding = mapOf(
    "1" to listOf(3, 4, 4, 3, 5, 4, 3),
    "2" to listOf(2, 2, 3, 2, 1, 2),
    "3" to listOf(4, 5, 4, 5, 4),
    "4" to listOf(1, 2, 1, 2, 1, 2, 1)
)
