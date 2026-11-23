package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.ranking.RankedRestaurant
import com.uniandes.campuseats.data.ranking.RankingCriterion
import com.uniandes.campuseats.data.ranking.RankingWindow
import com.uniandes.campuseats.ranking.RankingUiState
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)
private val Up = Color(0xFF2E9B57)
private val Down = Color(0xFFD64545)
private val Gold = Color(0xFFE0A526)
private val Silver = Color(0xFF9AA0A6)
private val Bronze = Color(0xFFB87333)
private const val MAX_SCORE = 5f

@Composable
fun RankingView(
    state: RankingUiState,
    onCriterion: (RankingCriterion) -> Unit,
    onWindow: (RankingWindow) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (Restaurant) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink, modifier = Modifier.size(18.dp))
            }
            Text("Top Ranking", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = OutfitFontFamily)
            RoundIconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Ink, modifier = Modifier.size(18.dp))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RankingCriterion.entries) { criterion ->
                Pill(criterion.label, criterion == state.criterion) { onCriterion(criterion) }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankingWindow.entries.forEach { window ->
                Pill(window.label, window == state.window) { onWindow(window) }
            }
        }

        val updatedAt = state.lastUpdated?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
        val status = when {
            state.refreshQueued -> "Refresh queued · it will update when you're back online"
            !state.online -> "Offline · showing saved ranking" + (updatedAt?.let { " from $it" } ?: "")
            updatedAt != null -> "Updated $updatedAt"
            else -> null
        }
        status?.let {
            Text(
                text = it,
                color = if (state.refreshQueued || !state.online) Orange else Muted,
                fontSize = 12.sp,
                fontWeight = if (state.refreshQueued || !state.online) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.items.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.restaurant.id }) { item ->
                        RankingRow(item) { onSelect(item.restaurant) }
                    }
                }

                state.loading -> CircularProgressIndicator(
                    color = Orange,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> Text(
                    text = if (state.online) "Not enough data to rank restaurants yet" else "You're offline and there is no saved data to rank",
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Orange else Color.White)
            .border(1.dp, if (selected) Orange else Outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = OutfitFontFamily
        )
    }
}

@Composable
private fun RankingRow(item: RankedRestaurant, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PositionBadge(item.position)
        Spacer(Modifier.width(10.dp))
        RestaurantImage(
            model = item.restaurant.image,
            contentDescription = item.restaurant.name,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.restaurant.name,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (item.score.toFloat() / MAX_SCORE).coerceIn(0f, 1f) },
                color = Orange,
                trackColor = Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.2f", item.score)} · ${item.reviewCount} ${if (item.reviewCount == 1) "rating" else "ratings"}",
                color = Muted,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        MovementTag(item.movement)
    }
}

@Composable
private fun PositionBadge(position: Int) {
    val color = when (position) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> Outline
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            color = if (position <= 3) Color.White else Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MovementTag(movement: Int?) {
    val (text, color) = when {
        movement == null -> return
        movement > 0 -> "▲ $movement" to Up
        movement < 0 -> "▼ ${-movement}" to Down
        else -> "–" to Muted
    }
    Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}
