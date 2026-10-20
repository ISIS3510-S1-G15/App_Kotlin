package com.uniandes.campuseats.analytics

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AnalyticsGateway(
    private val transport: AnalyticsTransport,
    private val scope: CoroutineScope,
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
        }
    }
}
