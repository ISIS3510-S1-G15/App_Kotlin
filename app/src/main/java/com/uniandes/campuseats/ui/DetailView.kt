package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.avgCrowding
import com.uniandes.campuseats.data.getCrowdingLevel
import com.uniandes.campuseats.sensor.formatDistance
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.util.Locale
import kotlin.math.roundToInt

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Line = Color(0xFFF0E8DE)
private val Outline = Color(0xFFE8E0D4)
private val LightMuted = Color(0xFFB8AC9E)
private val Gold = Color(0xFFF4A735)
private val Green = Color(0xFF3A8C4F)
private val BarOff = Color(0xFFD4CAC0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailView(
    restaurant: Restaurant,
    reviews: List<Review>,
    crowdingReports: List<Int>,
    isSaved: Boolean,
    distanceMeters: Float?,
    onToggleSave: () -> Unit,
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
    onSeeReviews: () -> Unit
) {
    val r = restaurant
    var activeMenu by remember(r.id) { mutableIntStateOf(0) }

    val reviewAvg = if (reviews.isNotEmpty()) {
        String.format(Locale.US, "%.1f", reviews.map { it.ratings.overall }.average())
    } else null

    val crowdAvg = avgCrowding(crowdingReports)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            AsyncImage(
                model = r.image,
                contentDescription = r.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.6f))))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    tint = Ink,
                    description = "Back",
                    onClick = onBack
                )
                HeroButton(
                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    tint = if (isSaved) Orange else Ink,
                    description = "Save",
                    onClick = onToggleSave
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (r.isOpen) "OPEN NOW" else "CLOSED",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (r.isOpen) Green else Muted)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Text(
                    text = r.category,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Info card (se superpone 24dp sobre el hero)
        Column(
            modifier = Modifier
                .offset(y = (-24).dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text(
                text = r.name,
                color = Ink,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = OutfitFontFamily
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Orange.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Text(
                    text = if (distanceMeters != null) "${r.location} · ${formatDistance(distanceMeters)}" else r.location,
                    color = Muted,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Line)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(label = "${if (reviews.isNotEmpty()) reviews.size else r.reviews} reviews", modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(14.dp))
                        StatValue(reviewAvg ?: r.rating.toString())
                    }
                }
                StatDivider()
                StatColumn(label = "Wait time", modifier = Modifier.weight(1f)) { StatValue(r.waitTime) }
                StatDivider()
                StatColumn(label = "Price range", modifier = Modifier.weight(1f)) { StatValue(r.price) }
            }
        }

        Column(modifier = Modifier.offset(y = (-24).dp)) {
            // Hours & tags
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = Orange, modifier = Modifier.size(15.dp))
                    Text(text = r.hours, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    r.tags.forEach { tag ->
                        Text(
                            text = tag,
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Cream)
                                .border(1.dp, Outline, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Crowding
            if (crowdAvg != null) {
                val level = getCrowdingLevel(crowdAvg)
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(level.bg)
                        .border(1.dp, level.border, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CrowdingBars(
                                value = crowdAvg.roundToInt(),
                                activeColor = level.barColor,
                                barWidth = 4f,
                                baseHeight = 6f,
                                step = 3f,
                                gap = 3f
                            )
                            Text(
                                text = "${level.label} right now",
                                color = level.color,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = OutfitFontFamily
                            )
                        }
                        CrowdingBadge(reports = crowdingReports, size = "md")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(BarOff)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((crowdAvg / 5).toFloat())
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(level.barColor)
                            )
                        }
                        val n = crowdingReports.size
                        Text(
                            text = "${String.format(Locale.US, "%.1f", crowdAvg)} / 5 · $n report${if (n != 1) "s" else ""}",
                            color = level.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Description
            Text(
                text = r.description,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )

            // Menu
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)) {
                Text(
                    text = "Menu",
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    r.menu.forEachIndexed { i, section ->
                        val active = activeMenu == i
                        Text(
                            text = section.category,
                            color = if (active) Color.White else Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) Orange else Color.White)
                                .then(if (active) Modifier else Modifier.border(1.dp, Outline, CircleShape))
                                .clickable { activeMenu = i }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    r.menu.getOrNull(activeMenu)?.items?.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = OutfitFontFamily
                                )
                                Text(
                                    text = item.description,
                                    color = Muted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(text = item.price, color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Review CTAs
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val n = reviews.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Outline, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSeeReviews),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "See $n Review${if (n != 1) "s" else ""}",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Orange)
                        .clickable(onClick = onWriteReview),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Write a Review",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroButton(icon: ImageVector, tint: Color, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatColumn(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        value()
        Text(text = label, color = LightMuted, fontSize = 10.sp)
    }
}

@Composable
private fun StatValue(text: String) {
    Text(text = text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Line)
    )
}
