package com.uniandes.campuseats.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView


object OfflineMap {
    private const val TILE_KEEP_MS = 14L * 24 * 60 * 60 * 1000 // los tiles guardados valen 14 días
    private const val CACHE_MAX_BYTES = 100L * 1024 * 1024
    private const val CACHE_TRIM_BYTES = 80L * 1024 * 1024

    private const val CAMPUS_LAT = 4.6025
    private const val CAMPUS_LON = -74.0655
    private const val RADIUS_DEG = 0.008
    private const val PREFETCH_MIN_ZOOM = 15
    private const val PREFETCH_MAX_ZOOM = 17

    @Volatile private var configured = false
    @Volatile private var prefetched = false

    fun configure(context: Context) {
        if (configured) return
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        val base = File(context.applicationContext.filesDir, "osmdroid")
        config.osmdroidBasePath = base
        config.osmdroidTileCache = File(base, "tiles")
        config.expirationOverrideDuration = TILE_KEEP_MS
        config.tileFileSystemCacheMaxBytes = CACHE_MAX_BYTES
        config.tileFileSystemCacheTrimBytes = CACHE_TRIM_BYTES
        configured = true
    }

    fun applyConnectivity(mapView: MapView, online: Boolean) {
        if (mapView.isUseDataConnection() != online) mapView.setUseDataConnection(online)
    }

    fun prefetchCampus(mapView: MapView) {
        if (prefetched) return
        prefetched = true
        try {
            val area = BoundingBox(
                CAMPUS_LAT + RADIUS_DEG, CAMPUS_LON + RADIUS_DEG,
                CAMPUS_LAT - RADIUS_DEG, CAMPUS_LON - RADIUS_DEG
            )
            CacheManager(mapView).downloadAreaAsync(mapView.context, area, PREFETCH_MIN_ZOOM, PREFETCH_MAX_ZOOM)
        } catch (e: Exception) {
            prefetched = false 
        }
    }
}

@Composable
fun OfflineMapNotice(modifier: Modifier = Modifier) {
    Text(
        text = "Offline · showing saved map areas",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1A1208).copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = OutfitFontFamily
    )
}