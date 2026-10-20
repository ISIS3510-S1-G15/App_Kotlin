package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.data.emptyProfile
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    private val failures = mutableListOf<String>()

    private fun viewModel(transport: AnalyticsTransport, scope: TestScope): AnalyticsViewModel {
        val gateway = AnalyticsGateway(transport, scope) { path, _ -> failures.add(path) }
        return AnalyticsViewModel(AnalyticsRepository(gateway) { "install-1" })
    }

    @Before
    fun reset() {
        failures.clear()
    }

    @Test
    fun filterUsageSendsFilterAndScreen() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        viewModel(transport, this).onFilterUsed("Vegan options", AnalyticsScreen.Search)
        advanceUntilIdle()
        assertEquals(
            listOf("/analytics/filter-usage" to mapOf("filter" to "Vegan options", "screen" to "search")),
            transport.requests
        )
    }

    @Test
    fun occupancyCheckAndSelectionMapTabToScreen() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val vm = viewModel(transport, this)
        vm.onRestaurantChecked("r1", 0)
        vm.onRestaurantSelected("r1", 2)
        advanceUntilIdle()
        assertEquals(
            listOf(
                "/analytics/occupancy-check" to mapOf("restaurant_id" to "r1", "screen" to "home", "action" to "checked"),
                "/analytics/occupancy-check" to mapOf("restaurant_id" to "r1", "screen" to "map", "action" to "selected")
            ),
            transport.requests
        )
    }

    @Test
    fun unknownTabFallsBackToHome() {
        assertEquals(AnalyticsScreen.Home, AnalyticsScreen.fromTab(99))
        assertEquals(AnalyticsScreen.Saved, AnalyticsScreen.fromTab(3))
        assertEquals(AnalyticsScreen.Profile, AnalyticsScreen.fromTab(4))
    }

    @Test
    fun profileEditSendsOneEventPerChangedField() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val old = emptyProfile.copy(dietaryRestrictions = listOf("Vegan"), cuisinePreferences = listOf("Asian"))
        val updated = old.copy(dietaryRestrictions = listOf("Halal"))
        viewModel(transport, this).onProfileSaved(old, updated)
        advanceUntilIdle()
        assertEquals(
            listOf("/analytics/profile-edit" to mapOf("install_id" to "install-1", "field" to "dietary_restrictions")),
            transport.requests
        )
    }

    @Test
    fun profileEditSendsBothFieldsWhenBothChange() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val updated = emptyProfile.copy(dietaryRestrictions = listOf("Vegan"), cuisinePreferences = listOf("Asian"))
        viewModel(transport, this).onProfileSaved(emptyProfile, updated)
        advanceUntilIdle()
        assertEquals(
            listOf("dietary_restrictions", "cuisine_preferences"),
            transport.requests.map { it.second["field"] }
        )
    }

    @Test
    fun profileEditIgnoresUnchangedAndReorderedSelections() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val old = emptyProfile.copy(dietaryRestrictions = listOf("Vegan", "Halal"))
        val updated = old.copy(dietaryRestrictions = listOf("Halal", "Vegan"), name = "Other")
        viewModel(transport, this).onProfileSaved(old, updated)
        advanceUntilIdle()
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun transportFailureIsSwallowedAndReported() = runTest(StandardTestDispatcher()) {
        viewModel(FakeTransport(IOException("offline")), this).onFilterUsed("Halal", AnalyticsScreen.Search)
        advanceUntilIdle()
        assertEquals(listOf("/analytics/filter-usage"), failures)
    }

    @Test
    fun apiErrorStatusIsSwallowedAndReported() = runTest(StandardTestDispatcher()) {
        viewModel(FakeTransport(ApiException(500, "boom")), this).onRestaurantChecked("r1", 2)
        advanceUntilIdle()
        assertEquals(listOf("/analytics/occupancy-check"), failures)
    }
}
