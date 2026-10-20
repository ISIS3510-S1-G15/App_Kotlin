package com.uniandes.campuseats.analytics

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface AnalyticsTransport {
    suspend fun post(path: String, body: Map<String, Any>)
}

class ApiException(val statusCode: Int, val body: String) : Exception("HTTP $statusCode: $body")

class HttpAnalyticsTransport(
    private val baseUrl: String,
    private val timeoutMs: Int = 5_000
) : AnalyticsTransport {

    override suspend fun post(path: String, body: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(JSONObject(body).toString().toByteArray()) }
                val status = connection.responseCode
                if (status !in 200..299) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw ApiException(status, error)
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
