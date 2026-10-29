package com.uniandes.campuseats.analytics

import android.util.Log
import com.uniandes.campuseats.net.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AnalyticsGateway(
    private val transport: AnalyticsTransport,
    private val scope: CoroutineScope,
    private val keepForLater: (suspend (String, Map<String, Any>) -> Unit)? = null,
    private val onFailure: (String, Throwable) -> Unit = { path, error ->
        Log.w("Analytics", "Could not log $path: $error")
    }
) {
    fun send(path: String, body: Map<String, Any>): Job = scope.launch {
        try {
            transport.post(path, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onFailure(path, e)
            if (e !is ApiException) keepForLater?.invoke(path, body)
        }
    }
}
