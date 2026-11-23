package com.uniandes.campuseats.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConnectivityMonitor {
    /** true solo si hay una red con internet VALIDADO (no portal cautivo). Fuente única de verdad de la app. */
    val online: StateFlow<Boolean>

    /** true si la red activa es de datos móviles / tarificada. */
    val metered: StateFlow<Boolean>
}

class ConnectivityObserver(context: Context) : ConnectivityMonitor {

    private val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val onlineState = MutableStateFlow(false)
    private val meteredState = MutableStateFlow(false)

    override val online: StateFlow<Boolean> = onlineState.asStateFlow()
    override val metered: StateFlow<Boolean> = meteredState.asStateFlow()

    init {
        refresh(currentCapabilities())
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh(manager.getNetworkCapabilities(network))

                // Se dispara cuando la red gana/pierde VALIDATED (portal cautivo, sin datos reales, etc.)
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                    refresh(capabilities)

                override fun onLost(network: Network) {
                    // No consultamos activeNetwork aqui: puede seguir devolviendo la red que se acaba de perder
                    onlineState.value = false
                    meteredState.value = false
                }

                override fun onUnavailable() {
                    onlineState.value = false
                }
            })
        }
    }

    private fun currentCapabilities(): NetworkCapabilities? =
        runCatching { manager.getNetworkCapabilities(manager.activeNetwork) }.getOrNull()

    private fun refresh(capabilities: NetworkCapabilities?) {
        onlineState.value = capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        meteredState.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
    }
}
