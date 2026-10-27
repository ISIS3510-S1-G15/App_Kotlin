package com.uniandes.campuseats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.location.Location
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Marker
import coil.compose.AsyncImage
import com.uniandes.campuseats.sensor.locationLabel
import com.uniandes.campuseats.data.CAMPUS_LATITUDE
import com.uniandes.campuseats.data.CAMPUS_LONGITUDE
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.closestOpenSpot
import com.uniandes.campuseats.data.currentMinutesOfDay
import com.uniandes.campuseats.data.isOnCampus
import com.uniandes.campuseats.data.walkingMinutes
import com.uniandes.campuseats.sensor.distanceMeters
import com.uniandes.campuseats.sensor.formatDistance
import java.util.Calendar
import com.uniandes.campuseats.data.restaurants
import com.uniandes.campuseats.ui.theme.OutfitFontFamily

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Muted = Color(0xFF7A6D5F)
private val BorderColor = Color(0xFFE8E0D4)
private val CardBorder = Color(0xFFF0E8DE)
private val Accent = Color(0xFFE8440A)
private val Green = Color(0xFF3A8C4F)
private val GreenBg = Color(0xFFE8F5EB)
private val ClosedBg = Color(0xFFF0EEEC)
private val StarColor = Color(0xFFF4A735)
private val Disabled = Color(0xFFB8AC9E)

private const val STAR_PATH = "M12 2L15.1 8.3L22 9.3L17 14.2L18.2 21L12 17.8L5.8 21L7 14.2L2 9.3L8.9 8.3L12 2Z"
private const val CHEVRON_PATH = "M9 18L15 12L9 6"

@Composable
private fun PathIcon(
    data: String,
    size: Dp,
    fill: Color? = null,
    stroke: Color? = null,
    strokeWidth: Float = 2f,
    round: Boolean = false
) {
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            if (fill != null) {
                drawPath(path, fill)
            }
            if (stroke != null) {
                drawPath(
                    path,
                    stroke,
                    style = Stroke(
                        width = strokeWidth,
                        cap = if (round) StrokeCap.Round else StrokeCap.Butt,
                        join = if (round) StrokeJoin.Round else StrokeJoin.Miter
                    )
                )
            }
        }
    }
}


@Composable
fun MapView(
    onSelect: (Restaurant) -> Unit,
    crowding: Map<String, List<Int>>,
    userLocation: Location?,
    hasLocationPermission: Boolean = true,
    onRetryLocation: () -> Unit = {},
    onFilterUsed: (String) -> Unit = {}
) {
    var selected by remember { mutableStateOf<Restaurant?>(null) }
    var filter by remember { mutableStateOf("all") }

    val distances = userLocation
        ?.let { loc -> restaurants.associate { it.id to distanceMeters(loc, it) } }
        .orEmpty()
    val openOrAll = if (filter == "all") restaurants else restaurants.filter { it.isOpen }
    val visible = if (distances.isEmpty()) openOrAll else openOrAll.sortedBy { distances.getValue(it.id) }
    val distanceToCampus = userLocation?.let { loc ->
        FloatArray(1).also {
            Location.distanceBetween(loc.latitude, loc.longitude, CAMPUS_LATITUDE, CAMPUS_LONGITUDE, it)
        }[0]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Campus Map",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                fontFamily = OutfitFontFamily
            )
            Text(
                text = "Universidad de los Andes — Campus Principal",
                fontSize = 12.sp,
                color = Muted
            )
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allActive = filter == "all"
            Text(
                text = "All Spots",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (allActive) Color.White else Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (allActive) Ink else Color.White)
                    .then(if (!allActive) Modifier.border(1.dp, BorderColor, CircleShape) else Modifier)
                    .clickable {
                        if (filter != "all") {
                            filter = "all"
                            onFilterUsed("All Spots")
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val openActive = filter == "open"
            Text(
                text = "Open Now",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (openActive) Color.White else Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (openActive) Green else Color.White)
                    .then(if (!openActive) Modifier.border(1.dp, BorderColor, CircleShape) else Modifier)
                    .clickable {
                        if (filter != "open") {
                            filter = "open"
                            onFilterUsed("Open Now")
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        val suggestion = if (distanceToCampus != null && isOnCampus(distanceToCampus)) {
            closestOpenSpot(
                restaurants,
                distances,
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                currentMinutesOfDay()
            )
        } else {
            null
        }
        when {
            userLocation == null && !hasLocationPermission -> LocationBanner(
                text = "Location unavailable. Allow location access to see the closest spots",
                actionLabel = "Retry",
                onAction = onRetryLocation
            )
            userLocation == null -> LocationBanner(text = "Finding your location…")
            distanceToCampus != null && !isOnCampus(distanceToCampus) -> LocationBanner(
                text = "You're ${formatDistance(distanceToCampus)} away from campus"
            )
            suggestion == null -> LocationBanner(text = "No spots are open near you right now")
            else -> {
                val spot = suggestion.restaurant
                val distanceLabel = "${formatDistance(suggestion.distanceMeters)} · ${walkingMinutes(suggestion.distanceMeters)} min walk"
                LocationBanner(
                    text = if (suggestion.meal != null) {
                        "${suggestion.meal.label} time · Closest for ${suggestion.meal.label.lowercase()}: ${spot.name} · $distanceLabel"
                    } else {
                        "Closest open spot: ${spot.name} · $distanceLabel"
                    },
                    actionLabel = "Show",
                    onAction = { selected = spot }
                )
            }
        }

        val mapShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(300.dp)
                .shadow(8.dp, mapShape)
                .clip(mapShape)
                .border(1.dp, CardBorder, mapShape)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    Configuration.getInstance().userAgentValue = context.packageName

                    OsmMapView(context).apply {
                        setMultiTouchControls(true)
                        controller.setZoom(17.0)
                        controller.setCenter(GeoPoint(4.6025, -74.0655))
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    // 1. Dibujamos los pines
                    val size = 50 // Tamaño en pixeles
                    val openBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                    val closedBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)

                    val openCanvas = android.graphics.Canvas(openBitmap)
                    val closedCanvas = android.graphics.Canvas(closedBitmap)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

                    // Pin Abierto
                    paint.color = android.graphics.Color.WHITE
                    openCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    paint.color = Green.toArgb() // Usamos el verde de tu paleta
                    openCanvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 6f, paint)

                    // Pin Cerrado
                    paint.color = android.graphics.Color.WHITE
                    closedCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    paint.color = android.graphics.Color.parseColor("#D32F2F") // Un rojo profesional
                    closedCanvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 6f, paint)

                    val openIcon = android.graphics.drawable.BitmapDrawable(mapView.context.resources, openBitmap)
                    val closedIcon = android.graphics.drawable.BitmapDrawable(mapView.context.resources, closedBitmap)

                    // 2. Asignamos los íconos a los restaurantes
                    visible.forEach { r ->
                        val isSelected = selected?.id == r.id
                        val geoPoint = GeoPoint(r.latitude, r.longitude)

                        val marker = Marker(mapView)
                        marker.position = geoPoint
                        marker.title = r.name
                        marker.snippet = "${r.waitTime} wait • ${if (r.isOpen) "Open" else "Closed"}"

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.icon = if (r.isOpen) openIcon else closedIcon

                        if (isSelected) marker.showInfoWindow()

                        marker.setOnMarkerClickListener { _, _ ->
                            selected = if (isSelected) null else r
                            true
                        }

                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Green)
                    )
                    Text(text = "Open", fontSize = 9.sp, color = Muted)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F))
                    )
                    Text(text = "Closed", fontSize = 9.sp, color = Muted)
                }
            }
        }

        selected?.let { sel ->
            val cardShape = RoundedCornerShape(16.dp)
            Box(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, cardShape)
                        .clip(cardShape)
                        .background(Color.White)
                        .border(1.dp, CardBorder, cardShape)
                        .clickable { onSelect(sel) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = sel.image,
                        contentDescription = sel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = sel.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontFamily = OutfitFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = if (sel.isOpen) "Open" else "Closed",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel.isOpen) Green else Muted,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sel.isOpen) GreenBg else ClosedBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = locationLabel(sel, userLocation),
                            fontSize = 11.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PathIcon(STAR_PATH, 10.dp, fill = StarColor)
                                Text(
                                    text = "${sel.rating}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                            Text(text = "• ${sel.waitTime}", fontSize = 11.sp, color = Muted)
                            CrowdingBadge(reports = crowding[sel.id] ?: emptyList(), size = "sm")
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        PathIcon(CHEVRON_PATH, 16.dp, stroke = Disabled, strokeWidth = 2f, round = true)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Nearby — ${visible.size} spots",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { r ->
                    val itemShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(Color.White)
                            .border(
                                1.dp,
                                if (selected?.id == r.id) Accent else CardBorder,
                                itemShape
                            )
                            .clickable { selected = r }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = categoryEmoji(r.category), fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontFamily = OutfitFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = distances[r.id]
                                    ?.let { "${formatDistance(it)} · ${walkingMinutes(it)} min walk · ${r.waitTime} wait" }
                                    ?: "${r.waitTime} wait",
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }
                        Text(
                            text = if (r.isOpen) "Open" else "Closed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (r.isOpen) Green else Muted,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (r.isOpen) GreenBg else ClosedBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationBanner(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Accent.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "📍", fontSize = 14.sp)
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

private fun categoryEmoji(cat: String): String {
    val map = mapOf(
        "Dining Hall" to "🍽",
        "Café" to "☕",
        "Asian" to "🍜",
        "Burgers" to "🍔",
        "Indian" to "🍛",
        "Smoothies" to "🥤"
    )
    return map[cat] ?: "🍴"
}
