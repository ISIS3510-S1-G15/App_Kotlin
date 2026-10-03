package com.uniandes.campuseats.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.uniandes.campuseats.data.Restaurant
import java.util.Locale
import kotlin.math.roundToInt

private const val UPDATE_INTERVAL_MS = 10_000L
private const val UPDATE_MIN_DISTANCE_M = 10f

/** Estado del sensor GPS que consumen las pantallas. */
class UserLocationState(
    val location: Location?,
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

/**
 * Sensor: lee la ubicación del teléfono (GPS y red) mientras la app está visible.
 * El seguimiento se activa en onStart y se detiene en onStop, y cada vez que se reactiva
 * vuelve a revisar qué proveedores están encendidos (por si el usuario prendió el GPS después).
 */
@SuppressLint("MissingPermission") // se verifica con hasPermission antes de pedir ubicaciones
@Composable
fun rememberUserLocation(): UserLocationState {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var location by remember { mutableStateOf<Location?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = context.hasLocationPermission()
    }

    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }

    DisposableEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@DisposableEffect onDispose { }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null

        fun stop() {
            listener?.let { manager.removeUpdates(it) }
            listener = null
        }

        fun start() {
            stop()
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

            // Última posición conocida para mostrar algo de inmediato
            providers
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { location = it }

            val newListener = object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    location = newLocation
                }

                // Se declaran porque en Android < 11 estos métodos no tienen implementación por defecto
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            providers.forEach {
                manager.requestLocationUpdates(it, UPDATE_INTERVAL_MS, UPDATE_MIN_DISTANCE_M, newListener)
            }
            listener = newListener
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }

        if (lifecycleOwner != null) {
            lifecycleOwner.lifecycle.addObserver(observer)
        } else {
            start()
        }

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            stop()
        }
    }

    return UserLocationState(
        location = location,
        hasPermission = hasPermission,
        requestPermission = {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    )
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Distancia en metros entre el usuario y un restaurante. */
fun distanceMeters(from: Location, restaurant: Restaurant): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, restaurant.latitude, restaurant.longitude, result)
    return result[0]
}

/**
 * Ubicación del restaurante con la distancia al usuario: "Plazoleta Lleras · 80 m".
 * Sin ubicación del usuario devuelve solo la ubicación del restaurante.
 */
fun locationLabel(restaurant: Restaurant, userLocation: Location?): String =
    if (userLocation == null) restaurant.location
    else "${restaurant.location} · ${formatDistance(distanceMeters(userLocation, restaurant))}"

/** "<10 m", "80 m" (redondeado a 10) o "1.2 km". */
fun formatDistance(meters: Float): String = when {
    meters < 10f -> "<10 m"
    meters < 995f -> "${(meters / 10).roundToInt() * 10} m"
    else -> String.format(Locale.US, "%.1f km", meters / 1000f)
}
