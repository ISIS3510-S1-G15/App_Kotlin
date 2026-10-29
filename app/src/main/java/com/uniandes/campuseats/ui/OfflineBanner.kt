package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.ui.theme.OutfitFontFamily

private val BannerInk = Color(0xFF1A1208)

@Composable
fun OfflineBanner(pendingChanges: Int, modifier: Modifier = Modifier) {
    val suffix = when (pendingChanges) {
        0 -> ""
        1 -> " · 1 change waiting to sync"
        else -> " · $pendingChanges changes waiting to sync"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BannerInk)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "You're offline$suffix",
            color = Color.White,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
fun PendingSyncBanner(pendingChanges: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BannerInk.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (pendingChanges == 1) "Syncing 1 change…" else "Syncing $pendingChanges changes…",
            color = Color.White,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
