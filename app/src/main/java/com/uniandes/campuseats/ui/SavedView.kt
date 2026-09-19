package com.uniandes.campuseats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Restaurant
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

private const val PLUS_PATH = "M12 5V19M5 12H19"
private const val BOOKMARK_PATH = "M5 3H19C19.6 3 20 3.4 20 4V21L12 17L4 21V4C4 3.4 4.4 3 5 3Z"
private const val PIN_PATH = "M12 2C8.7 2 6 4.7 6 8C6 13 12 22 12 22C12 22 18 13 18 8C18 4.7 15.3 2 12 2Z"
private const val STAR_PATH = "M12 2L15.1 8.3L22 9.3L17 14.2L18.2 21L12 17.8L5.8 21L7 14.2L2 9.3L8.9 8.3L12 2Z"

private val DIETARY = listOf("Vegan", "Gluten-free", "Halal", "Vegetarian")

@Composable
private fun PathIcon(
    data: String,
    size: Dp,
    fill: Color? = null,
    stroke: Color? = null,
    strokeWidth: Float = 2f,
    alpha: Float = 1f,
    round: Boolean = false
) {
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            if (fill != null) {
                drawPath(path, fill, alpha = alpha)
            }
            if (stroke != null) {
                drawPath(
                    path,
                    stroke,
                    alpha = alpha,
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
fun SavedView(
    onSelect: (Restaurant) -> Unit,
    crowding: Map<String, List<Int>>
) {
    var dietaryFilters by remember { mutableStateOf(listOf<String>()) }
    val saved = restaurants.filter { it.saved }

    val toggleDiet = { d: String ->
        dietaryFilters = if (dietaryFilters.contains(d)) {
            dietaryFilters.filter { it != d }
        } else {
            dietaryFilters + d
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    fontFamily = OutfitFontFamily
                )
                Text(
                    text = "${saved.size} spots saved",
                    fontSize = 12.sp,
                    color = Muted
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, BorderColor, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                PathIcon(PLUS_PATH, 16.dp, stroke = Ink, strokeWidth = 2f, round = true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = "Dietary Preferences",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DIETARY.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { d ->
                            val active = dietaryFilters.contains(d)
                            val shape = RoundedCornerShape(12.dp)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shape)
                                    .background(if (active) Accent else Color.White)
                                    .border(1.dp, if (active) Accent else BorderColor, shape)
                                    .clickable { toggleDiet(d) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = dietEmoji(d), fontSize = 16.sp)
                                Text(
                                    text = d,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color.White else Ink
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Ink)
                .padding(16.dp)
        ) {
            Text(
                text = "Open Right Now",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                restaurants.filter { it.isOpen }.take(3).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = categoryEmoji(r.category), fontSize = 14.sp)
                            Text(
                                text = r.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Green)
                            )
                            Text(
                                text = r.hours.split('-')[1].trim(),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Your Spots",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (saved.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(1.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        PathIcon(BOOKMARK_PATH, 28.dp, stroke = Disabled, strokeWidth = 1.8f)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No saved spots yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Bookmark restaurants to find them here",
                        fontSize = 12.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    saved.forEach { r ->
                        SavedCard(
                            restaurant = r,
                            onSelect = onSelect,
                            crowdingReports = crowding[r.id] ?: emptyList()
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Recently Viewed",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                restaurants.filter { !it.saved }.take(4).forEach { r ->
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .shadow(1.dp, shape)
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, CardBorder, shape)
                            .clickable { onSelect(r) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            AsyncImage(
                                model = r.image,
                                contentDescription = r.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                        )
                                    )
                            )
                            Text(
                                text = if (r.isOpen) "Open" else "Closed",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(if (r.isOpen) Green else Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = r.name,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontFamily = OutfitFontFamily
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PathIcon(STAR_PATH, 8.dp, fill = StarColor)
                                Text(text = "${r.rating}", fontSize = 10.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCard(
    restaurant: Restaurant,
    onSelect: (Restaurant) -> Unit,
    crowdingReports: List<Int>
) {
    val r = restaurant
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .clickable { onSelect(r) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            AsyncImage(
                model = r.image,
                contentDescription = r.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Ink.copy(alpha = 0.6f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = r.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PathIcon(STAR_PATH, 10.dp, fill = StarColor)
                    Text(
                        text = "${r.rating}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                PathIcon(BOOKMARK_PATH, 13.dp, fill = Accent)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PathIcon(PIN_PATH, 10.dp, fill = Accent, alpha = 0.6f)
                Text(text = r.location, fontSize = 11.sp, color = Muted)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                CrowdingBadge(reports = crowdingReports, size = "sm")
            }
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

private fun dietEmoji(d: String): String {
    val map = mapOf(
        "Vegan" to "🌱",
        "Gluten-free" to "🌾",
        "Halal" to "☪️",
        "Vegetarian" to "🥗"
    )
    return map[d] ?: "✓"
}
