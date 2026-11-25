package com.uniandes.campuseats.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uniandes.campuseats.AppServices
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.net.attemptOrNull

/** Envia la cola de salida y el perfil pendiente aunque la app este cerrada. */
class OutboxSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val services = AppServices.get(applicationContext)
        if (services.sessionStore.token == null) return Result.success() // sin sesion no hay nada que enviar

        val profileSynced = attemptOrNull { services.profileSynchronizer.syncPending() } ?: false
        val flush = attemptOrNull { services.outboxFlusher.flush() }

        if (flush?.authExpired == true) {
            // No podemos arreglar un token vencido en segundo plano: la app lo pide al abrirse y la cola se conserva
            services.markSessionExpired()
            return Result.success()
        }
        val stillPending = !profileSynced || flush == null || flush.remaining > 0
        return if (stillPending && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}

/** Revalida el cache de lectura en segundo plano (preferencia "background prefetch"). */
class CacheRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val services = AppServices.get(applicationContext)
        if (services.sessionStore.token == null) return Result.success()

        val restaurants = attemptOrNull { services.restaurantRepository.revalidate() }
        attemptOrNull { services.catalogRepository.revalidate() }
        attemptOrNull { services.reviewRepository.revalidateSummaries() }
        attemptOrNull { services.occupancyRepository.revalidate() }?.let {
            attemptOrNull { services.localStore.saveCrowding(it) }
        }

        val ids = (restaurants ?: services.restaurantRepository.cached()).map { it.id }
        if (ids.isNotEmpty()) {
            val fetched = attemptOrNull { services.reviewRepository.revalidateAll(ids) }.orEmpty()
            if (fetched.isNotEmpty()) {
                val current = attemptOrNull { services.localStore.loadReviews() }.orEmpty()
                val merged = ids.flatMap { id -> fetched[id] ?: current.filter { it.restaurantId == id } }
                attemptOrNull { services.localStore.saveReviews(merged) }
            }
        }
        attemptOrNull { services.localStore.saveDocument("last_sync", "ok", CachePolicy().now()) }
        return Result.success()
    }
}

class RankingRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val services = AppServices.get(applicationContext)
        if (services.sessionStore.token == null) return Result.success()

        val refreshed = attemptOrNull { services.rankingRepository.refreshInBackground() } ?: false
        return if (!refreshed && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
