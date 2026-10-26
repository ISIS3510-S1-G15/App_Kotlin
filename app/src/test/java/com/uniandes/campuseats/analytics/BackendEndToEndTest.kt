package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.auth.AuthRepository
import com.uniandes.campuseats.auth.AuthUser
import com.uniandes.campuseats.auth.SessionStorage
import com.uniandes.campuseats.data.emptyProfile
import com.uniandes.campuseats.net.HttpRemoteApi
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class BackendEndToEndTest {
    private val baseUrl: String? = System.getenv("ANALYTICS_E2E_URL")

    private class MemorySession : SessionStorage {
        override var token: String? = null
        override var user: AuthUser? = null
        override fun clear() {
            token = null
            user = null
        }
    }

    private fun get(path: String): String {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    @Test
    fun eventsReachTheRealBackend() {
        assumeTrue(baseUrl != null)
        val failures = mutableListOf<String>()
        runBlocking {
            val session = MemorySession()
            val api = HttpRemoteApi(baseUrl!!) { session.token }
            AuthRepository(api, session).register(
                "E2E Tester",
                "e2e-${System.currentTimeMillis()}@uniandes.edu.co",
                "password123"
            )
            val job = Job()
            val gateway = AnalyticsGateway(
                HttpAnalyticsTransport(api),
                CoroutineScope(job + Dispatchers.IO)
            ) { path, error -> failures.add("$path: $error") }
            val viewModel = AnalyticsViewModel(AnalyticsRepository(gateway) { "e2e-install" })
            viewModel.onFilterUsed("Vegan options", AnalyticsScreen.Search)
            viewModel.onFilterUsed("Dining Hall", AnalyticsScreen.Home)
            viewModel.onOccupancyChecked("r-e2e", true)
            viewModel.onGoDecision("r-e2e")
            viewModel.onProfileSaved(
                emptyProfile,
                emptyProfile.copy(dietaryRestrictions = listOf("Vegan"), cuisinePreferences = listOf("Asian"))
            )
            job.children.toList().forEach { it.join() }
            delay(500)
        }
        assertEquals(emptyList<String>(), failures)
        println("E2E filters=" + get("/analytics/filter-usage"))
        println("E2E occupancy=" + get("/analytics/occupancy-check-rate"))
        println("E2E profile=" + get("/analytics/profile-edits/by-field"))
    }

    @Test
    fun unreachableBackendDoesNotCrash() = runTest {
        val failures = mutableListOf<String>()
        val gateway = AnalyticsGateway(HttpAnalyticsTransport("http://127.0.0.1:9", 500), this) { path, _ ->
            failures.add(path)
        }
        gateway.send("/analytics/filter-usage", mapOf("filter" to "x", "screen" to "home")).join()
        assertEquals(listOf("/analytics/filter-usage"), failures)
    }
}
