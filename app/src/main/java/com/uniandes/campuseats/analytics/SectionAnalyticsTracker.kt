package com.uniandes.campuseats.analytics

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SectionAnalyticsTracker(
    private val transport: AnalyticsTransport,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val maxQueueSize: Int = DEFAULT_MAX_QUEUE_SIZE,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
    private val onFailure: (Throwable) -> Unit = { Log.w("Analytics", "Could not log section events: $it") }
) {
    private val lock = Any()
    private val activeVisits = mutableMapOf<AppSection, String>()
    private val pending = ArrayDeque<SectionEvent>()
    private var flushing = false

    fun onSectionShown(section: AppSection) {
        val visitId = synchronized(lock) {
            if (section in activeVisits) return
            newId().also { activeVisits[section] = it }
        }
        enqueue(SectionEvent.Type.View, section, null, visitId)
    }

    fun onSectionLeft(section: AppSection) {
        synchronized(lock) { activeVisits.remove(section) }
    }

    fun onInteraction(section: AppSection, action: SectionAction, visitSection: AppSection = section) {
        val visitId = synchronized(lock) { activeVisits[visitSection] } ?: return
        enqueue(SectionEvent.Type.Interaction, section, action, visitId)
    }

    private fun enqueue(type: SectionEvent.Type, section: AppSection, action: SectionAction?, visitId: String) {
        val event = SectionEvent(newId(), type, section, action, visitId, formatTimestamp(clock()))
        synchronized(lock) {
            pending.addLast(event)
            while (pending.size > maxQueueSize) pending.removeFirst()
        }
        scheduleFlush()
    }

    private fun scheduleFlush() {
        synchronized(lock) {
            if (flushing || pending.isEmpty()) return
            flushing = true
        }
        scope.launch {
            try {
                drain()
            } finally {
                synchronized(lock) { flushing = false }
                scheduleFlush()
            }
        }
    }

    private suspend fun drain() {
        while (true) {
            val batch = synchronized(lock) { pending.take(BATCH_SIZE) }
            if (batch.isEmpty()) return
            send(batch)
            synchronized(lock) { pending.removeAll(batch.toSet()) }
        }
    }

    private suspend fun send(batch: List<SectionEvent>) {
        val body = mapOf<String, Any>("events" to batch.map { it.toBody() })
        for (attempt in 1..maxAttempts) {
            try {
                transport.post(EVENTS_PATH, body)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(e)
                if (attempt < maxAttempts) delay(retryDelayMs * attempt)
            }
        }
    }

    private fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    companion object {
        const val EVENTS_PATH = "/analytics/sections/events"
        const val DEFAULT_MAX_QUEUE_SIZE = 100
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_RETRY_DELAY_MS = 2_000L
        const val BATCH_SIZE = 50
    }
}
