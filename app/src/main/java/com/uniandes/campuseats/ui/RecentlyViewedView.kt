package com.uniandes.campuseats.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.remote.RecentView
import com.uniandes.campuseats.data.remote.RecentViewsRepository
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HistoryCream = Color(0xFFF5EBE0)
private val HistoryInk = Color(0xFF1A1208)
private val HistoryMuted = Color(0xFF7A6D5F)
private val HistoryFaint = Color(0xFFADA195)
private val HistoryOpen = Color(0xFF1E9E2F)
private val HistoryClosed = Color(0xFFA39A90)
private val HistoryAccent = Color(0xFFE8440A)
private val HistoryBanner = Color(0xFF1F1915)

data class RecentlyViewedEntry(val restaurant: Restaurant, val view: RecentView)

fun resolveRecentlyViewed(views: List<RecentView>, restaurants: List<Restaurant>): List<RecentlyViewedEntry> {
    val byId = restaurants.associateBy { it.id }
    return views
        .sortedByDescending { it.lastViewedAt }
        .mapNotNull { view -> byId[view.restaurantId]?.let { RecentlyViewedEntry(it, view) } }
}

fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun dayLabel(millis: Long, now: Long): String {
    val today = startOfDay(now)
    val day = startOfDay(millis)
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    return when {
        day >= today -> "TODAY"
        day == yesterday -> "YESTERDAY"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH).format(Date(millis)).uppercase(Locale.ENGLISH)
    }
}

private sealed interface HistoryRow {
    data class Header(val label: String) : HistoryRow
    data class Item(val entry: RecentlyViewedEntry) : HistoryRow
}

@Composable
fun RecentlyViewedView(
    entries: List<RecentlyViewedEntry>,
    updatedAt: Long?,
    isOnline: Boolean,
    pendingChanges: Int,
    isSyncing: Boolean,
    onSelect: (Restaurant) -> Unit,
    onRemove: (Restaurant) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var removeCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    val removeCandidate = entries.firstOrNull { it.restaurant.id == removeCandidateId }?.restaurant
    val now = System.currentTimeMillis()
    val rows = buildList {
        entries.groupBy { dayLabel(it.view.lastViewedAt, now) }.forEach { (label, group) ->
            add(HistoryRow.Header(label))
            group.forEach { add(HistoryRow.Item(it)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryCream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAction(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = HistoryInk, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HISTORY",
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    color = HistoryMuted
                )
                Text(
                    text = "Recently viewed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = HistoryInk,
                    fontFamily = OutfitFontFamily
                )
            }
            CircleAction(
                enabled = entries.isNotEmpty(),
                onClick = { confirmClear = true }
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Clear history",
                    tint = if (entries.isNotEmpty()) HistoryInk else HistoryFaint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (!isOnline) OfflineHistoryBanner()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = updatedAt?.let { "Updated at ${formatClock(it)}" } ?: "Not synced yet",
                fontSize = 13.sp,
                color = HistoryFaint
            )
            val syncLabel = when {
                isSyncing && isOnline -> "Syncing…"
                pendingChanges == 1 -> "1 change waiting to sync"
                pendingChanges > 1 -> "$pendingChanges changes waiting to sync"
                else -> null
            }
            if (syncLabel != null) {
                Text(
                    text = syncLabel,
                    fontSize = 13.sp,
                    color = HistoryAccent
                )
            }
        }

        if (entries.isNotEmpty()) {
            Text(
                text = "Saved on this phone · last ${RecentViewsRepository.MAX_ITEMS} places for ${RecentViewsRepository.MAX_AGE_DAYS} days · Long-press to remove",
                fontSize = 12.sp,
                color = HistoryFaint,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        if (entries.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(rows, key = { row ->
                    when (row) {
                        is HistoryRow.Header -> "header_${row.label}"
                        is HistoryRow.Item -> "item_${row.entry.restaurant.id}"
                    }
                }) { row ->
                    when (row) {
                        is HistoryRow.Header -> Text(
                            text = row.label,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HistoryMuted,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                        is HistoryRow.Item -> HistoryCard(
                            entry = row.entry,
                            onSelect = onSelect,
                            onLongPress = { removeCandidateId = it.id }
                        )
                    }
                }
            }
        }
    }

    if (removeCandidate != null) {
        AlertDialog(
            onDismissRequest = { removeCandidateId = null },
            title = { Text("Remove from history?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isOnline) "${removeCandidate.name} will be removed from your recently viewed places."
                    else "${removeCandidate.name} will be removed now and synced when you're back online."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    removeCandidateId = null
                    onRemove(removeCandidate)
                }) {
                    Text("Remove", color = HistoryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeCandidateId = null }) {
                    Text("Cancel", color = HistoryMuted)
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isOnline) "All your recently viewed restaurants will be removed."
                    else "Your history will be cleared on this phone now and synced when you're back online."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) {
                    Text("Clear", color = HistoryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = HistoryMuted)
                }
            }
        )
    }
}

@Composable
private fun CircleAction(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun OfflineHistoryBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HistoryBanner)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = "You're offline. This is the history saved on this phone. Removed places will sync when you're back online.",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    entry: RecentlyViewedEntry,
    onSelect: (Restaurant) -> Unit,
    onLongPress: (Restaurant) -> Unit
) {
    val r = entry.restaurant
    val times = entry.view.viewCount
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .combinedClickable(
                onClick = { onSelect(r) },
                onLongClick = { onLongPress(r) }
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RestaurantImage(
            model = r.image,
            contentDescription = r.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = r.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = HistoryInk,
                    fontFamily = OutfitFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (r.isOpen) "Open" else "Closed",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (r.isOpen) HistoryOpen else HistoryClosed
                )
            }
            Text(
                text = "${r.category} · ${r.location}",
                fontSize = 15.sp,
                color = HistoryMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Viewed $times ${if (times == 1) "time" else "times"} · last at ${formatClock(entry.view.lastViewedAt)}",
                fontSize = 14.sp,
                color = HistoryFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = HistoryFaint, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No recently viewed spots",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = HistoryInk,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Restaurants you open will appear here",
            fontSize = 13.sp,
            color = HistoryMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
