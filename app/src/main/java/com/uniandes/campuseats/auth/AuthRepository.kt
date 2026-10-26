package com.uniandes.campuseats.auth

import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.RemoteApi
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

class AuthRepository(
    private val api: RemoteApi,
    private val storage: SessionStorage
) {
    suspend fun register(name: String, email: String, password: String): AuthUser =
        authenticate("/auth/register", mapOf("name" to name.trim(), "email" to email.trim(), "password" to password))

    suspend fun login(email: String, password: String): AuthUser =
        authenticate("/auth/login", mapOf("email" to email.trim(), "password" to password))

    suspend fun restoreSession(): AuthUser? {
        val saved = storage.user
        if (storage.token == null || saved == null) return null
        return try {
            AuthUser.fromJson(api.get("/auth/me")).also { storage.user = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.statusCode == 401) {
                storage.clear()
                null
            } else {
                saved
            }
        } catch (e: Exception) {
            saved
        }
    }

    fun logout() = storage.clear()

    private suspend fun authenticate(path: String, body: Map<String, Any>): AuthUser {
        val response = try {
            api.post(path, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw AuthException(e.detail ?: messageFor(e.statusCode))
        } catch (e: SocketTimeoutException) {
            throw AuthException("The server took too long to answer. Try again.")
        } catch (e: Exception) {
            throw AuthException("Could not connect to the server. Check your internet connection.")
        }
        val user = AuthUser.fromJson(response.getJSONObject("user"))
        storage.token = response.getString("access_token")
        storage.user = user
        return user
    }

    private fun messageFor(statusCode: Int) = when (statusCode) {
        401 -> "Incorrect email or password"
        409 -> "An account with this email already exists"
        422 -> "Check the data you entered"
        else -> "Something went wrong ($statusCode). Try again."
    }
}
