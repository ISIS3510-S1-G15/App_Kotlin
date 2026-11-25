package com.uniandes.campuseats.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.uniandes.campuseats.data.prefs.UserPreferences
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Programa trabajo en segundo plano con WorkManager, respetando las preferencias del usuario:
 *  - [scheduleOutboxSync]: envia la cola de salida (y el perfil pendiente) en cuanto haya red,
 *    aunque la app este cerrada. Reintenta con backoff exponencial.
 *  - [applyPeriodicRefresh]: precarga/revalida el cache cada pocas horas si "background prefetch" esta activo.
 */
class SyncScheduler(
    private val context: Context,
    private val prefs: () -> UserPreferences
) {
    private fun workManager() = runCatching { WorkManager.getInstance(context) }.getOrNull()

    fun scheduleOutboxSync() {
        val network = if (prefs().syncOnlyOnWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager()?.enqueueUniqueWork(OUTBOX_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleRankingRefresh() {
        val network = if (prefs().syncOnlyOnWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<RankingRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager()?.enqueueUniqueWork(RANKING_WORK, ExistingWorkPolicy.KEEP, request)
    }

    fun rankingRefreshPending(): Flow<Boolean> {
        val manager = workManager() ?: return flowOf(false)
        return manager.getWorkInfosForUniqueWorkFlow(RANKING_WORK).map { infos ->
            infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        }
    }

    fun applyPeriodicRefresh() {
        val current = prefs()
        val manager = workManager() ?: return
        if (!current.backgroundPrefetch) {
            manager.cancelUniqueWork(REFRESH_WORK)
            return
        }
        // Con ahorro de datos o "solo Wi-Fi" nunca se precarga por datos moviles
        val network = if (current.syncOnlyOnWifi || current.dataSaver) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = PeriodicWorkRequestBuilder<CacheRefreshWorker>(REFRESH_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(network)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        manager.enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelAll() {
        workManager()?.run {
            cancelUniqueWork(OUTBOX_WORK)
            cancelUniqueWork(REFRESH_WORK)
            cancelUniqueWork(RANKING_WORK)
        }
    }

    private companion object {
        const val OUTBOX_WORK = "outbox-sync"
        const val REFRESH_WORK = "cache-refresh"
        const val RANKING_WORK = "ranking-refresh"
        const val BACKOFF_SECONDS = 30L
        const val REFRESH_HOURS = 3L
    }
}
