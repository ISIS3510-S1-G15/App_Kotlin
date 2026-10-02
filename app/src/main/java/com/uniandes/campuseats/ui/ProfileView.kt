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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import com.uniandes.campuseats.data.Profile
import kotlin.math.roundToInt

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Line = Color(0xFFF0E8DE)
private val Outline = Color(0xFFE8E0D4)
private val Green = Color(0xFF3A8C4F)
private val GreenBg = Color(0xFFE8F5EB)
private val GreenBorder = Color(0xFFC5E0CB)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileView(
    profile: Profile,
    onRetakeSurvey: () -> Unit,
    onBack: () -> Unit
) {
    val initials = if (profile.name.isNotEmpty()) profile.name.take(2).uppercase() else "ME"
    val completeness = listOf(
        profile.dietaryRestrictions.isNotEmpty(),
        profile.frequency.isNotEmpty(),
        profile.usualMealTimes.isNotEmpty(),
        profile.budget.isNotEmpty(),
        profile.topPriorities.isNotEmpty()
    ).count { it }
    val completenessFraction = completeness / 6f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Outline, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "My Profile",
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = OutfitFontFamily
            )
            Text(
                text = "Edit",
                color = Orange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onRetakeSurvey)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Ink)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Orange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                }
                Column {
                    Text(
                        text = profile.name.ifEmpty { "Uniandino" },
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                    Text(
                        text = "Universidad de los Andes",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile completeness",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(completenessFraction * 100).roundToInt()}%",
                    color = Orange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(completenessFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Orange)
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                value = if (profile.cuisinePreferences.isNotEmpty()) profile.cuisinePreferences.size.toString() else "—",
                label = "Cuisines",
                emoji = "🌍",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = profile.frequency.ifEmpty { "—" },
                label = "Frequency",
                emoji = "🕐",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = profile.budget.ifEmpty { "—" },
                label = "Budget",
                emoji = "💰",
                modifier = Modifier.weight(1f)
            )
        }

        if (profile.dietaryRestrictions.isNotEmpty()) {
            Section(title = "Dietary Restrictions", emoji = "🌿") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.dietaryRestrictions.forEach { d ->
                        Text(
                            text = "✓ $d",
                            color = Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GreenBg)
                                .border(1.dp, GreenBorder, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (profile.cuisinePreferences.isNotEmpty()) {
            Section(title = "Cuisine Preferences", emoji = "🌍") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.cuisinePreferences.forEach { c ->
                        Text(
                            text = c,
                            color = Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Outline, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (profile.usualMealTimes.isNotEmpty()) {
            Section(title = "Usual Meal Times", emoji = "⏰") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.usualMealTimes.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Line, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Orange)
                            )
                            Text(
                                text = t,
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (profile.topPriorities.isNotEmpty()) {
            Section(title = "Top Priorities", emoji = "⭐") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.topPriorities.forEachIndexed { i, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Line, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Cream)
                                    .border(1.dp, Outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    color = Orange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = p,
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (profile.foodsToAvoid.isNotEmpty()) {
            Section(title = "Foods to Avoid", emoji = "🚫") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = profile.foodsToAvoid.joinToString(", "), color = Ink, fontSize = 13.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onRetakeSurvey),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retake Food Preferences Survey",
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, emoji: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Text(
            text = value,
            color = Ink,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OutfitFontFamily,
            textAlign = TextAlign.Center
        )
        Text(text = label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun Section(title: String, emoji: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = emoji, fontSize = 15.sp)
            Text(
                text = title,
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        }
        content()
    }
}
