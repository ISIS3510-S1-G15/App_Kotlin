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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
private fun myReviewCategories(): List<Pair<String, (ReviewRatings) -> Int>> {
    val selectors = mapOf<String, (ReviewRatings) -> Int>(
        "taste" to { r -> r.taste },
        "attention" to { r -> r.attention },
        "price" to { r -> r.price },
        "options" to { r -> r.options }
    )
    return LocalCatalog.current.options("review_categories").mapNotNull { option ->
        selectors[option.value]?.let { (option.detail ?: option.label) to it }
    }
}

private fun formatReviewDate(date: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
    if (parsed != null) SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed) else date
} catch (e: Exception) {
    date
}

private fun Double.oneDecimal() = String.format(Locale.US, "%.1f", this)

@Composable
fun MyReviewsView(
    reviews: List<Review>,
    restaurantsById: Map<String, Restaurant>,
    isLoading: Boolean,
    onSelectRestaurant: (Restaurant) -> Unit,
    onBack: () -> Unit
) {
    val averageGiven = if (reviews.isNotEmpty()) reviews.map { it.ratings.overall }.average() else 0.0

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackCircle(onBack)
            Column {
                Text(
                    text = "PROFILE",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "My Reviews",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily
                )
            }
        }

        if (isLoading && reviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Orange)
            }
            return
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ink)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${reviews.size}",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                    Text(
                        text = "review${if (reviews.size != 1) "s" else ""} written",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (reviews.isNotEmpty()) averageGiven.oneDecimal() else "—",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = OutfitFontFamily
                        )
                    }
                    Text(
                        text = "Avg. rating given",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            // Empty State
            if (reviews.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "✍️", fontSize = 40.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = "No reviews yet",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                    Text(
                        text = "Reviews you write for campus spots will appear here.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Review Cards
            reviews.forEach { review ->
                val restaurant = restaurantsById[review.restaurantId]
                MyReviewCard(
                    review = review,
                    restaurantName = restaurant?.name ?: "Restaurant",
                    onClick = { restaurant?.let(onSelectRestaurant) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyReviewCard(
    review: Review,
    restaurantName: String,
    onClick: () -> Unit
) {
    val isPending = review.date == "Pending sync..."
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPending) Cream else Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurantName,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
                if (isPending) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, null, tint = Orange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Pending sync", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(text = formatReviewDate(review.date), color = LightMuted, fontSize = 10.sp)
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
            myReviewCategories().chunked(2).forEach { pair ->
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
                            Text(text = label, color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            MyReviewStars(value = selector(review.ratings).toDouble(), size = 9)
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

@Composable
private fun MyReviewStars(value: Double, size: Int) {
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