package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.getCrowdingLevel
import com.uniandes.campuseats.ui.theme.OutfitFontFamily

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)
private val LightMuted = Color(0xFFB8AC9E)
private val BarIdle = Color(0xFFD4CAC0)

private data class LevelOption(val value: Int, val label: String, val desc: String, val icon: String)

private val Levels = listOf(
    LevelOption(1, "Empty", "Plenty of seats", "🪑"),
    LevelOption(2, "Quiet", "Few people around", "😌"),
    LevelOption(3, "Moderate", "Some tables taken", "🙂"),
    LevelOption(4, "Busy", "Hard to find a seat", "😅"),
    LevelOption(5, "Packed", "Completely full", "😬")
)

/** Hoja inferior que pregunta qué tan lleno está el restaurante antes de abrir el detalle. */
@Composable
fun CrowdingModal(
    restaurant: Restaurant,
    onSubmit: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var selected by remember(restaurant.id) { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim: tocar fuera equivale a Skip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Cream)
                // Evita que los toques dentro de la hoja lleguen al scrim
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Outline)
            )

            Row(
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = restaurant.image,
                    contentDescription = restaurant.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(
                        text = "QUICK CHECK",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "How crowded is ${restaurant.name}?",
                        color = Ink,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                }
            }

            Column(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Levels.forEach { option ->
                    LevelRow(
                        option = option,
                        isSelected = selected == option.value,
                        onClick = { selected = option.value }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Outline, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSkip),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Skip", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                val enabled = selected != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .then(if (enabled) Modifier.shadow(8.dp, RoundedCornerShape(16.dp)) else Modifier)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (enabled) Orange else Outline)
                        .clickable(enabled = enabled) { selected?.let(onSubmit) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Report & Continue",
                        color = if (enabled) Color.White else LightMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelRow(option: LevelOption, isSelected: Boolean, onClick: () -> Unit) {
    val level = getCrowdingLevel(option.value.toDouble())
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) level.bg else Color.White)
            .border(1.dp, if (isSelected) level.border else Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CrowdingBars(
            value = option.value,
            activeColor = if (isSelected) level.barColor else BarIdle,
            inactiveColor = Outline,
            barWidth = 4f,
            baseHeight = 8f,
            step = 4f,
            gap = 2f
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${option.value}  — ${option.label}",
                color = if (isSelected) level.color else Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = option.desc,
                color = if (isSelected) level.color else Muted,
                fontSize = 11.sp
            )
        }
        Text(text = option.icon, fontSize = 20.sp)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(level.barColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
