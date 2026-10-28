package com.uniandes.campuseats.analytics

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionAnalyticsTrackerTest {

    private class FlakyTransport(private var failures: Int) : AnalyticsTransport {
        val bodies = mutableListOf<Map<String, Any>>()
        override suspend fun post(path: String, body: Map<String, Any>) {
            bodies.add(body)
            if (failures > 0) {
                failures--
                throw IOException("offline")
            }
        }
    }

    private fun eventsOf(body: Map<String, Any>): List<Map<String, Any>> =
        (body["events"] as List<*>).map { event ->
            @Suppress("UNCHECKED_CAST")
            event as Map<String, Any>
        }

    private fun events(transport: FakeTransport): List<Map<String, Any>> =
        transport.requests.flatMap { (_, body) -> eventsOf(body) }

    private fun tracker(
        transport: AnalyticsTransport,
        scope: TestScope,
        maxQueueSize: Int = 100,
        failures: MutableList<Throwable> = mutableListOf()
    ): SectionAnalyticsTracker {
        var counter = 0
        return SectionAnalyticsTracker(
            transport = transport,
            scope = scope,
            clock = { 1_700_000_000_000 },
            newId = { "id-${counter++}" },
            maxQueueSize = maxQueueSize,
            retryDelayMs = 10,
            onFailure = { failures.add(it) }
        )
    }

    @Test
    fun showingASectionSendsOneViewWithVisitAndTimestamp() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        val sent = events(transport)
        assertEquals(1, sent.size)
        assertEquals("section_view", sent[0]["event_type"])
        assertEquals("map", sent[0]["section"])
        assertEquals("2023-11-14T22:13:20.000Z", sent[0]["client_timestamp"])
        assertTrue(sent[0]["visit_id"] != null && sent[0]["event_id"] != null)
        assertEquals(SectionAnalyticsTracker.EVENTS_PATH, transport.requests[0].first)
    }

    @Test
    fun repeatedShowWhileVisitActiveDoesNotDuplicate() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        repeat(5) { tracker.onSectionShown(AppSection.Favorites) }
        advanceUntilIdle()
        assertEquals(1, events(transport).size)
    }

    @Test
    fun leavingAndReturningStartsANewVisit() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Map)
        tracker.onSectionLeft(AppSection.Map)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        val sent = events(transport)
        assertEquals(2, sent.size)
        assertNotEquals(sent[0]["visit_id"], sent[1]["visit_id"])
    }

    @Test
    fun interactionCarriesTheActiveVisitId() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Filters)
        tracker.onInteraction(AppSection.Filters, SectionAction.FiltersApplied)
        advanceUntilIdle()
        val sent = events(transport)
        assertEquals(2, sent.size)
        assertEquals("section_interaction", sent[1]["event_type"])
        assertEquals("filters_applied", sent[1]["action"])
        assertEquals(sent[0]["visit_id"], sent[1]["visit_id"])
    }

    @Test
    fun interactionWithoutActiveVisitIsIgnored() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onInteraction(AppSection.Map, SectionAction.MarkerSelected)
        advanceUntilIdle()
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun favoriteInteractionUsesTheDetailsVisit() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Details)
        tracker.onInteraction(AppSection.Favorites, SectionAction.FavoriteAdded, AppSection.Details)
        advanceUntilIdle()
        val sent = events(transport)
        assertEquals("favorites", sent[1]["section"])
        assertEquals(sent[0]["visit_id"], sent[1]["visit_id"])
    }

    @Test
    fun networkFailureNeverThrowsAndRetriesAreBounded() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport(IOException("offline"))
        val failures = mutableListOf<Throwable>()
        val tracker = tracker(transport, this, failures = failures)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        assertEquals(SectionAnalyticsTracker.DEFAULT_MAX_ATTEMPTS, transport.requests.size)
        assertEquals(SectionAnalyticsTracker.DEFAULT_MAX_ATTEMPTS, failures.size)
    }

    @Test
    fun retryReusesTheSameEventId() = runTest(StandardTestDispatcher()) {
        val transport = FlakyTransport(failures = 2)
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        assertEquals(3, transport.bodies.size)
        assertEquals(1, transport.bodies.map { eventsOf(it).first()["event_id"] }.toSet().size)
    }

    @Test
    fun queueIsBounded() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this, maxQueueSize = 3)
        AppSection.values().forEach { tracker.onSectionShown(it) }
        tracker.onInteraction(AppSection.Map, SectionAction.MarkerSelected)
        advanceUntilIdle()
        assertEquals(3, events(transport).size)
    }

    @Test
    fun callsReturnBeforeAnyNetworkWork() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport()
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Map)
        assertTrue(transport.requests.isEmpty())
        advanceUntilIdle()
        assertEquals(1, transport.requests.size)
    }
}
