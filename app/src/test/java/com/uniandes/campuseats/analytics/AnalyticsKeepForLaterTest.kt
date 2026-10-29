package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.net.ApiException
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsKeepForLaterTest {

    private val kept = mutableListOf<Pair<String, Map<String, Any>>>()

    private val keep: suspend (String, Map<String, Any>) -> Unit = { path, body -> kept.add(path to body) }

    private fun gateway(transport: AnalyticsTransport, scope: TestScope) =
        AnalyticsGateway(transport, scope, keep) { _, _ -> }

    private fun tracker(transport: AnalyticsTransport, scope: TestScope) = SectionAnalyticsTracker(
        transport = transport,
        scope = scope,
        maxAttempts = 2,
        retryDelayMs = 10,
        onFailure = {},
        keepForLater = keep
    )

    @Test
    fun networkFailureKeepsTheEventForLater() = runTest(StandardTestDispatcher()) {
        gateway(FakeTransport(IOException("offline")), this).send("/analytics/filter-usage", mapOf("filter" to "Vegan"))
        advanceUntilIdle()
        assertEquals(listOf("/analytics/filter-usage" to mapOf<String, Any>("filter" to "Vegan")), kept)
    }

    @Test
    fun serverRejectionDoesNotKeepTheEvent() = runTest(StandardTestDispatcher()) {
        gateway(FakeTransport(ApiException(422, "")), this).send("/analytics/filter-usage", mapOf("filter" to "x"))
        advanceUntilIdle()
        assertTrue(kept.isEmpty())
    }

    @Test
    fun successDoesNotKeepAnything() = runTest(StandardTestDispatcher()) {
        gateway(FakeTransport(), this).send("/analytics/filter-usage", mapOf("filter" to "x"))
        advanceUntilIdle()
        assertTrue(kept.isEmpty())
    }

    @Test
    fun trackerKeepsTheBatchWhenAllAttemptsFailOffline() = runTest(StandardTestDispatcher()) {
        val transport = FakeTransport(IOException("offline"))
        val tracker = tracker(transport, this)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        assertEquals(2, transport.requests.size)
        val (path, body) = kept.single()
        assertEquals(SectionAnalyticsTracker.EVENTS_PATH, path)
        assertEquals(1, (body["events"] as List<*>).size)
    }

    @Test
    fun trackerDoesNotKeepTheBatchWhenTheServerRejectsIt() = runTest(StandardTestDispatcher()) {
        val tracker = tracker(FakeTransport(ApiException(422, "")), this)
        tracker.onSectionShown(AppSection.Map)
        advanceUntilIdle()
        assertTrue(kept.isEmpty())
    }
}
