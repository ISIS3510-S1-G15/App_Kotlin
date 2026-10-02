package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.text.SimpleDateFormat
import java.util.Date
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

private const val MAX_COMMENT = 300

private val DietaryOptions = listOf("Vegan options", "Vegetarian options", "Gluten-free", "Lactose-free", "Nut-free", "Halal", "Kosher")

private val RatingLabels = listOf(
    "taste" to "Taste & Food Quality",
    "attention" to "Service & Attention",
    "price" to "Value for Money",
    "options" to "Dietary Options"
)

private fun ratingLabel(v: Int) = listOf("", "Poor", "Fair", "Good", "Great", "Excellent").getOrElse(v) { "" }

/** Encuesta de calificación del restaurante: 4 categorías con estrellas, dietas y comentario. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WriteReviewView(
    restaurant: Restaurant,
    authorName: String,
    onSubmit: (Review) -> Unit,
    onBack: () -> Unit
) {
    var ratings by remember { mutableStateOf(mapOf("taste" to 0, "attention" to 0, "price" to 0, "options" to 0)) }
    var dietaryOptions by remember { mutableStateOf(listOf<String>()) }
    var comment by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val allRated = ratings.values.all { it > 0 }
    val avgRating = if (allRated) String.format(Locale.US, "%.1f", ratings.values.average()) else "—"

    if (submitted) {
        SubmittedScreen(restaurantName = restaurant.name, avgRating = avgRating, onBack = onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Header
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackCircle(onBack)
            Column {
                Text(
                    text = "WRITE A REVIEW",
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
                    fontFamily = OutfitFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card {
                CardTitle("Rate your experience", bottom = 16.dp)
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    RatingLabels.forEach { (key, label) ->
                        val value = ratings.getValue(key)
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (value > 0) {
                                    Text(text = ratingLabel(value), color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            StarRating(value = value, onChange = { ratings = ratings + (key to it) })
                        }
                    }
                }

                if (allRated) {
                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Overall rating", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
                            Text(
                                text = avgRating,
                                color = Ink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = OutfitFontFamily
                            )
                        }
                    }
                }
            }

            Card {
                CardTitle("Available dietary options", bottom = 4.dp)
                Text(
                    text = "Which special diets does this restaurant accommodate?",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DietaryOptions.forEach { opt ->
                        val isSelected = opt in dietaryOptions
                        Text(
                            text = if (isSelected) "✓ $opt" else opt,
                            color = if (isSelected) Color.White else Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Green else Cream)
                                .border(1.dp, if (isSelected) Green else Outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    dietaryOptions = if (isSelected) dietaryOptions - opt else dietaryOptions + opt
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Card {
                CardTitle("Final comments", bottom = 4.dp)
                Text(
                    text = "Share your experience with other students",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                CommentField(value = comment, onValueChange = { comment = it.take(MAX_COMMENT) })
                Text(
                    text = "${comment.length}/$MAX_COMMENT",
                    color = LightMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Submit
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            if (!allRated) {
                Text(
                    text = "Please rate all 4 categories to submit",
                    color = LightMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(if (allRated) Modifier.shadow(8.dp, RoundedCornerShape(16.dp)) else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (allRated) Orange else Outline)
                    .clickable(enabled = allRated) {
                        submitted = true
                        onSubmit(
                            Review(
                                id = "r${System.currentTimeMillis()}",
                                restaurantId = restaurant.id,
                                author = authorName.ifBlank { "Anonymous" },
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                ratings = ReviewRatings(
                                    taste = ratings.getValue("taste"),
                                    attention = ratings.getValue("attention"),
                                    price = ratings.getValue("price"),
                                    options = ratings.getValue("options")
                                ),
                                dietaryOptions = dietaryOptions,
                                comment = comment.trim()
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Submit Review",
                    color = if (allRated) Color.White else LightMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
            }
        }
    }
}

@Composable
private fun SubmittedScreen(restaurantName: String, avgRating: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(Orange),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "¡Gracias!",
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OutfitFontFamily
        )
        Text(
            text = buildAnnotatedString {
                append("Your review of ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink)) { append(restaurantName) }
                append(" has been submitted.")
            },
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(22.dp))
            Text(
                text = avgRating,
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = OutfitFontFamily
            )
            Text(text = "your overall rating", color = Muted, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back to Restaurant",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        }
    }
}

@Composable
private fun StarRating(value: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { star ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "$star stars",
                tint = if (value >= star) Gold else Outline,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onChange(star) }
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
private fun CardTitle(text: String, bottom: Dp) {
    Text(
        text = text,
        color = Ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        fontFamily = OutfitFontFamily,
        modifier = Modifier.padding(bottom = bottom)
    )
}

@Composable
private fun CommentField(value: String, onValueChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        minLines = 4,
        textStyle = TextStyle(color = Ink, fontSize = 13.sp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cream)
                    .border(1.dp, if (focused) Orange else Outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(text = "What did you enjoy? What could be better?", color = LightMuted, fontSize = 13.sp)
                }
                inner()
            }
        }
    )
}

@Composable
internal fun BackCircle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = Ink, modifier = Modifier.size(22.dp))
    }
}
