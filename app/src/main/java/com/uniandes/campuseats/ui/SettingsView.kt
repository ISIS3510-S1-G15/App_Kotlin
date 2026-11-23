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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.uniandes.campuseats.AppServices
import com.uniandes.campuseats.data.cache.CacheStats
import com.uniandes.campuseats.data.prefs.CacheFreshness
import com.uniandes.campuseats.data.prefs.UserPreferences
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.util.Locale

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)

/**
 * Preferencias del usuario: sincronizacion, cache (memoria + disco) y busqueda.
 * Es "tonta": recibe el estado y notifica los cambios; MainActivity los guarda en DataStore.
 */
@Composable
fun SettingsView(
    preferences: UserPreferences,
    onChange: ((UserPreferences) -> UserPreferences) -> Unit,
    objectCache: CacheStats,
    documentCache: CacheStats,
    diskCache: AppServices.DiskCacheReport,
    pendingChanges: Int,
    online: Boolean,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink, modifier = Modifier.size(18.dp))
            }
            Text("Settings", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = OutfitFontFamily)
            Spacer(Modifier.width(36.dp))
        }

        Section("Sync") {
            SwitchRow(
                title = "Sync only on Wi-Fi",
                subtitle = "Pending changes are sent in the background only on unmetered networks",
                checked = preferences.syncOnlyOnWifi,
                onCheckedChange = { value -> onChange { it.copy(syncOnlyOnWifi = value) } }
            )
            SwitchRow(
                title = "Background prefetch",
                subtitle = "Refresh restaurants, reviews and crowding every few hours so the app opens ready offline",
                checked = preferences.backgroundPrefetch,
                onCheckedChange = { value -> onChange { it.copy(backgroundPrefetch = value) } }
            )
            SwitchRow(
                title = "Data saver",
                subtitle = "On mobile data, only show images that are already cached",
                checked = preferences.dataSaver,
                onCheckedChange = { value -> onChange { it.copy(dataSaver = value) } }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        pendingChanges == 0 -> "Everything is synced"
                        pendingChanges == 1 -> "1 change waiting to sync"
                        else -> "$pendingChanges changes waiting to sync"
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
                ActionButton(
                    label = if (online) "Sync now" else "Offline",
                    enabled = online && pendingChanges > 0,
                    onClick = onSyncNow
                )
            }
        }

        Section("Cache") {
            SwitchRow(
                title = "Keep cache on disk",
                subtitle = "Off = memory only: faster and private, but nothing survives closing the app",
                checked = preferences.persistCacheOnDisk,
                onCheckedChange = { value -> onChange { it.copy(persistCacheOnDisk = value) } }
            )
            ChipRow(
                title = "Data freshness",
                subtitle = "How long cached data is considered up to date",
                options = CacheFreshness.entries.map { it to freshnessLabel(it) },
                selected = preferences.cacheFreshness,
                onSelect = { value -> onChange { it.copy(cacheFreshness = value) } }
            )
            ChipRow(
                title = "Memory cache size",
                subtitle = "RAM used by the in-memory cache and images",
                options = UserPreferences.MEMORY_CACHE_OPTIONS_MB.map { it to "$it MB" },
                selected = preferences.memoryCacheMb,
                onSelect = { value -> onChange { it.copy(memoryCacheMb = value) } }
            )
            StatsBlock("Memory · parsed objects", objectCache)
            StatsBlock("Memory · documents", documentCache)
            Text(
                text = "Disk · ${diskCache.documents} documents · ${formatBytes(diskCache.bytes)}",
                color = Muted,
                fontSize = 12.sp
            )
            ActionButton(label = "Clear cache", enabled = true, onClick = onClearCache)
        }

        Section("Search & filters") {
            ChipRow(
                title = "Recent searches to keep",
                subtitle = null,
                options = UserPreferences.RECENT_SEARCH_OPTIONS.map { it to it.toString() },
                selected = preferences.maxRecentSearches,
                onSelect = { value -> onChange { it.copy(maxRecentSearches = value) } }
            )
            SwitchRow(
                title = "Remember Discover filters",
                subtitle = "Restore your last category and filters when you reopen the app",
                checked = preferences.rememberHomeFilters,
                onCheckedChange = { value -> onChange { it.copy(rememberHomeFilters = value) } }
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Orange)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(title: String, subtitle: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun StatsBlock(title: String, stats: CacheStats) {
    Column {
        Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${stats.size}/${stats.maxEntries} entries · ${stats.hits} hits · ${stats.misses} misses · " +
                "${String.format(Locale.US, "%.0f", stats.hitRate * 100)}% hit rate · ${stats.evictions} evicted",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Orange else Outline)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) Color.White else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

private fun freshnessLabel(value: CacheFreshness) = when (value) {
    CacheFreshness.SHORT -> "Short (refresh often)"
    CacheFreshness.NORMAL -> "Normal"
    CacheFreshness.LONG -> "Long (save data)"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
