package com.uniandes.campuseats.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConnectivityMonitor {
    val online: StateFlow<Boolean>
}

class ConnectivityObserver(context: Context) : ConnectivityMonitor {

    private val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val state = MutableStateFlow(hasInternet())

    override val online: StateFlow<Boolean> = state.asStateFlow()

    init {
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    state.value = true
                }

                override fun onLost(network: Network) {
                    state.value = hasInternet()
                }

                override fun onUnavailable() {
                    state.value = false
                }
            })
        }
    }

    private fun hasInternet(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
