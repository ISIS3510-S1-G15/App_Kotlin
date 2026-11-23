package com.uniandes.campuseats.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private val ImagePlaceholder = ColorPainter(Color(0xFFE8E0D4))

/**
 * Imagen de restaurante con cache de Coil (memoria + disco, ver CampusEatsApplication) y fallback visible:
 * sin conexion (o con ahorro de datos y una imagen sin cachear) se ve un bloque neutro en vez de un hueco vacio.
 */
@Composable
fun RestaurantImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = ImagePlaceholder,
        error = ImagePlaceholder
    )
}
