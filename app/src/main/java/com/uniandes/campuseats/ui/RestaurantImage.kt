package com.uniandes.campuseats.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

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
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Dp? = null
) {
    val source: Any? = if (thumbnailSize == null) {
        model
    } else {
        val context = LocalContext.current
        val pixels = with(LocalDensity.current) { thumbnailSize.roundToPx() }
        remember(model, pixels) {
            ImageRequest.Builder(context)
                .data(model)
                .size(Size(pixels, pixels))
                .build()
        }
    }
    AsyncImage(
        model = source,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = ImagePlaceholder,
        error = ImagePlaceholder
    )
}
