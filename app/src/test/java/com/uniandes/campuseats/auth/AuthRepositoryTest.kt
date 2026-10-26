package com.uniandes.campuseats.auth

import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.net.FakeRemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthRepositoryTest {

    private class MemorySession : SessionStorage {
        override var token: String? = null
        override var user: AuthUser? = null
        override fun clear() {
            token = null
            user = null
        }
    }

    private val tokenResponse =
        """{"access_token":"jwt-1","token_type":"bearer","user":{"id":7,"name":"Ana","email":"ana@uniandes.edu.co"}}"""

    @Test
    fun loginStoresTokenAndUser() = runTest {
        val api = FakeRemoteApi().apply { respond("/auth/login", tokenResponse) }
        val storage = MemorySession()
        val user = AuthRepository(api, storage).login(" ana@uniandes.edu.co ", "password1")
        assertEquals(AuthUser(7, "Ana", "ana@uniandes.edu.co"), user)
        assertEquals("jwt-1", storage.token)
        assertEquals(user, storage.user)
        assertEquals(mapOf("email" to "ana@uniandes.edu.co", "password" to "password1"), api.calls.single().third)
    }

    @Test
    fun registerSendsNameEmailAndPassword() = runTest {
        val api = FakeRemoteApi().apply { respond("/auth/register", tokenResponse) }
        AuthRepository(api, MemorySession()).register(" Ana ", "ana@uniandes.edu.co", "password1")
        assertEquals(
            mapOf("name" to "Ana", "email" to "ana@uniandes.edu.co", "password" to "password1"),
            api.calls.single().third
        )
    }

    @Test
    fun serverDetailBecomesTheErrorMessage() = runTest {
        val api = FakeRemoteApi(failure = ApiException(409, """{"detail":"An account with this email already exists"}"""))
        val error = assertThrows(AuthException::class.java) {
            kotlinx.coroutines.runBlocking { AuthRepository(api, MemorySession()).register("Ana", "ana@uniandes.edu.co", "password1") }
        }
        assertEquals("An account with this email already exists", error.message)
    }

    @Test
    fun missingDetailFallsBackToStatusMessage() = runTest {
        val api = FakeRemoteApi(failure = ApiException(401, ""))
        val error = assertThrows(AuthException::class.java) {
            kotlinx.coroutines.runBlocking { AuthRepository(api, MemorySession()).login("ana@uniandes.edu.co", "password1") }
        }
        assertEquals("Incorrect email or password", error.message)
    }

    @Test
    fun networkFailureIsReportedAsConnectionProblem() = runTest {
        val api = FakeRemoteApi(failure = IOException("offline"))
        val error = assertThrows(AuthException::class.java) {
            kotlinx.coroutines.runBlocking { AuthRepository(api, MemorySession()).login("ana@uniandes.edu.co", "password1") }
        }
        assertEquals("Could not connect to the server. Check your internet connection.", error.message)
    }

    @Test
    fun restoreWithoutSavedSessionReturnsNull() = runTest {
        assertNull(AuthRepository(FakeRemoteApi(), MemorySession()).restoreSession())
    }

    @Test
    fun restoreRefreshesUserWhenTokenIsValid() = runTest {
        val api = FakeRemoteApi().apply { respond("/auth/me", """{"id":7,"name":"Ana M","email":"ana@uniandes.edu.co"}""") }
        val storage = MemorySession().apply {
            token = "jwt-1"
            user = AuthUser(7, "Ana", "ana@uniandes.edu.co")
        }
        assertEquals(AuthUser(7, "Ana M", "ana@uniandes.edu.co"), AuthRepository(api, storage).restoreSession())
    }

    @Test
    fun restoreClearsSessionWhenTokenIsRejected() = runTest {
        val storage = MemorySession().apply {
            token = "old"
            user = AuthUser(7, "Ana", "ana@uniandes.edu.co")
        }
        val api = FakeRemoteApi(failure = ApiException(401, ""))
        assertNull(AuthRepository(api, storage).restoreSession())
        assertNull(storage.token)
        assertNull(storage.user)
    }

    @Test
    fun restoreKeepsSavedSessionWhenOffline() = runTest {
        val saved = AuthUser(7, "Ana", "ana@uniandes.edu.co")
        val storage = MemorySession().apply {
            token = "jwt-1"
            user = saved
        }
        assertEquals(saved, AuthRepository(FakeRemoteApi(failure = IOException("offline")), storage).restoreSession())
        assertEquals("jwt-1", storage.token)
    }

    @Test
    fun logoutClearsStorage() {
        val storage = MemorySession().apply {
            token = "jwt-1"
            user = AuthUser(7, "Ana", "ana@uniandes.edu.co")
        }
        AuthRepository(FakeRemoteApi(), storage).logout()
        assertNull(storage.token)
        assertNull(storage.user)
    }
}
