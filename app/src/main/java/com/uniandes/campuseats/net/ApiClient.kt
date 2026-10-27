package com.uniandes.campuseats.net

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ApiException(val statusCode: Int, val body: String) : Exception("HTTP $statusCode: $body") {
    val detail: String? = runCatching {
        when (val raw = JSONObject(body).opt("detail")) {
            is String -> raw
            is JSONArray -> raw.getJSONObject(0).getString("msg").removePrefix("Value error, ")
            else -> null
        }
    }.getOrNull()
}

interface RemoteApi {
    suspend fun get(path: String): JSONObject
    suspend fun post(path: String, body: Map<String, Any>): JSONObject
    suspend fun put(path: String, body: Map<String, Any>? = null): JSONObject
    suspend fun delete(path: String): JSONObject
}

class HttpRemoteApi(
    private val baseUrl: String,
    private val timeoutMs: Int = 15_000,
    private val tokenProvider: () -> String? = { null }
) : RemoteApi {

    override suspend fun get(path: String) = request("GET", path, null)

    override suspend fun post(path: String, body: Map<String, Any>) = request("POST", path, body)

    override suspend fun put(path: String, body: Map<String, Any>?) = request("PUT", path, body ?: emptyMap())

    override suspend fun delete(path: String) = request("DELETE", path, null)

    private suspend fun request(method: String, path: String, body: Map<String, Any>?): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                tokenProvider()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(JSONObject(body).toString().toByteArray()) }
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw ApiException(status, error)
                }
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            } finally {
                connection.disconnect()
            }
        }
}

suspend fun <T> attemptOrNull(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
