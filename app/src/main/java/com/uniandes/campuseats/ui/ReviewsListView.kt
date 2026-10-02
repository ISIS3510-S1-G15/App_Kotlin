package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Line = Color(0xFFF0E8DE)
private val Outline = Color(0xFFE8E0D4)
private val LightMuted = Color(0xFFB8AC9E)
private val Gold = Color(0xFFF4A735)
private val Green = Color(0xFF3A8C4F)
private val GreenBg = Color(0xFFE8F5EB)
private val GreenBorder = Color(0xFFC5E0CB)

private val Categories: List<Pair<String, (ReviewRatings) -> Int>> = listOf(
    "Taste" to { r: ReviewRatings -> r.taste },
    "Service" to { r: ReviewRatings -> r.attention },
    "Value" to { r: ReviewRatings -> r.price },
    "Diet Options" to { r: ReviewRatings -> r.options }
)

private fun formatDate(date: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
    if (parsed != null) SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed) else date
} catch (e: Exception) {
    date
}

private fun Double.oneDecimal() = String.format(Locale.US, "%.1f", this)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewsListView(
    restaurant: Restaurant,
    reviews: List<Review>,
    onBack: () -> Unit,
    onWriteReview: () -> Unit
) {
    val overall = if (reviews.isNotEmpty()) reviews.map { it.ratings.overall }.average() else 0.0
    val topDiets = reviews
        .flatMap { it.dietaryOptions }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackCircle(onBack)
                Column {
                    Text(
                        text = "REVIEWS",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = restaurant.name,
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                }
            }
            Text(
                text = "+ Write",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Orange)
                    .clickable(onClick = onWriteReview)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ink)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (reviews.isNotEmpty()) overall.oneDecimal() else "—",
                            color = Color.White,
                            fontSize = 42.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = OutfitFontFamily
                        )
                        Stars(value = overall, size = 14)
                        Text(
                            text = "${reviews.size} review${if (reviews.size != 1) "s" else ""}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Categories.forEach { (label, selector) ->
                            val v = if (reviews.isNotEmpty()) reviews.map { selector(it.ratings) }.average() else 0.0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(72.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((v / 5).toFloat())
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Gold)
                                    )
                                }
                                Text(
                                    text = if (v > 0) v.oneDecimal() else "—",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(22.dp)
                                )
                            }
                        }
                    }
                }

                if (topDiets.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Text(
                        text = "COMMONLY NOTED",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        topDiets.forEach { (diet, count) ->
                            Text(
                                text = "$diet · $count",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Empty state
            if (reviews.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "⭐", fontSize = 40.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = "No reviews yet",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                    Text(
                        text = "Be the first to review ${restaurant.name}",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )
                    Text(
                        text = "Write the first review",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Orange)
                            .clickable(onClick = onWriteReview)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            reviews.forEach { review -> ReviewCard(review) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(review: Review) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Author row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.author.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                }
                Column {
                    Text(text = review.author, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = formatDate(review.date), color = LightMuted, fontSize = 10.sp)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Cream)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(12.dp))
                Text(
                    text = review.ratings.overall.oneDecimal(),
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily
                )
            }
        }

        // Category breakdown (2x2)
        Column(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Categories.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { (label, selector) ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Cream)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = Muted,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Stars(value = selector(review.ratings).toDouble(), size = 9)
                        }
                    }
                }
            }
        }

        if (review.dietaryOptions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                review.dietaryOptions.forEach { d ->
                    Text(
                        text = "✓ $d",
                        color = Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GreenBg)
                            .border(1.dp, GreenBorder, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (review.comment.isNotBlank()) {
            Text(text = review.comment, color = Ink, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

/** Estrellas de solo lectura; igual que el prototipo, una media estrella cuenta como llena. */
@Composable
private fun Stars(value: Double, size: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { s ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (value >= s - 0.5) Gold else Outline,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}
