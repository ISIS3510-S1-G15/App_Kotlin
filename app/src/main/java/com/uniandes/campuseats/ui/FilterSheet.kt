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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.ui.theme.OutfitFontFamily

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)
private val LightMuted = Color(0xFFB8AC9E)
private val Green = Color(0xFF3A8C4F)

private val PriceOptions = listOf("$", "$$", "$$$")
private val RatingOptions = listOf(0.0 to "Any", 4.0 to "4.0+", 4.5 to "4.5+")

/** Filtros extra de la lista de Home (se suman a la categoría elegida). */
data class HomeFilters(
    val openNow: Boolean = false,
    val savedOnly: Boolean = false,
    val prices: Set<String> = emptySet(),
    val minRating: Double = 0.0
) {
    val activeCount: Int
        get() = listOf(openNow, savedOnly, prices.isNotEmpty(), minRating > 0.0).count { it }
}

/**
 * Hoja inferior con los filtros. Trabaja sobre un borrador y solo aplica al tocar
 * "Show N spots"; [countFor] calcula cuántos resultados daría el borrador.
 */
@Composable
fun FilterSheet(
    initial: HomeFilters,
    countFor: (HomeFilters) -> Int,
    onApply: (HomeFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(24.dp, sheetShape)
                .clip(sheetShape)
                .background(Cream)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily
                )
                Text(
                    text = "Clear all",
                    color = if (draft.activeCount > 0) Orange else LightMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { draft = HomeFilters() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                ToggleRow("Open now", "Only places serving right now", draft.openNow) {
                    draft = draft.copy(openNow = it)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Outline))
                ToggleRow("Saved only", "Spots you bookmarked", draft.savedOnly) {
                    draft = draft.copy(savedOnly = it)
                }

            }

            SectionLabel("Price range")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriceOptions.forEach { price ->
                    val active = price in draft.prices
                    OptionChip(text = price, active = active, modifier = Modifier.weight(1f)) {
                        draft = draft.copy(prices = if (active) draft.prices - price else draft.prices + price)
                    }
                }
            }

            SectionLabel("Minimum rating")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingOptions.forEach { (value, label) ->
                    OptionChip(
                        text = if (value > 0) "★ $label" else label,
                        active = draft.minRating == value,
                        modifier = Modifier.weight(1f)
                    ) {
                        draft = draft.copy(minRating = value)
                    }
                }
            }

            val count = countFor(draft)
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Orange)
                    .clickable { onApply(draft) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Show $count spot${if (count != 1) "s" else ""}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Muted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Outline,
                uncheckedBorderColor = Outline
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = OutfitFontFamily,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun OptionChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Ink else Color.White)
            .border(1.dp, if (active) Ink else Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.White else Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
