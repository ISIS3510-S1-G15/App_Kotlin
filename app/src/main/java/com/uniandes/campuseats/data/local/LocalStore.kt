package com.uniandes.campuseats.data.local

import androidx.room.withTransaction
import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.outbox.OutboxCodec
import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.outbox.OutboxEntry
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.data.outbox.OutboxStore
import com.uniandes.campuseats.data.ranking.RankingCriterion
import com.uniandes.campuseats.data.ranking.RankingWindow
import com.uniandes.campuseats.data.ranking.SnapshotRow
import com.uniandes.campuseats.data.ranking.StoredRanking
import com.uniandes.campuseats.data.remote.ProfileLocalSource
import com.uniandes.campuseats.data.remote.StoredProfile

class LocalStore(
    private val database: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis
) : ProfileLocalSource, OutboxStore {

    private val dao = database.dao()

    suspend fun loadReviews(): List<Review> = dao.reviews().map { it.toReview() }

    suspend fun saveReviews(reviews: List<Review>) {
        val rows = reviews.mapIndexed { index, review -> review.toEntity(index) }
        database.withTransaction {
            dao.clearReviews()
            dao.insertReviews(rows)
        }
    }

    suspend fun loadCrowding(): Map<String, List<Int>> =
        dao.crowdingReports().groupBy({ it.restaurantId }, { it.level })

    suspend fun saveCrowding(crowding: Map<String, List<Int>>) {
        val rows = crowding.flatMap { (restaurantId, levels) ->
            levels.map { CrowdingReportEntity(restaurantId = restaurantId, level = it) }
        }
        database.withTransaction {
            dao.clearCrowding()
            dao.insertCrowding(rows)
        }
    }

    suspend fun loadSavedIds(): Set<String> = dao.savedRestaurants().map { it.restaurantId }.toSet()

    suspend fun saveSavedIds(ids: Set<String>) {
        database.withTransaction {
            dao.clearSaved()
            dao.insertSaved(ids.map { SavedRestaurantEntity(it) })
        }
    }

    suspend fun loadRecentSearches(): List<String> = dao.recentSearches().map { it.name }

    suspend fun saveRecentSearches(names: List<String>) {
        database.withTransaction {
            dao.clearRecentSearches()
            dao.insertRecentSearches(names.mapIndexed { index, name -> RecentSearchEntity(index, name) })
        }
    }

    override suspend fun loadProfileRecord(): StoredProfile? =
        dao.profile()?.let { StoredProfile(it.toProfile(), it.pendingSync) }

    override suspend fun saveProfile(profile: Profile, pendingSync: Boolean) {
        dao.upsertProfile(profile.toEntity().copy(pendingSync = pendingSync))
    }

    override suspend fun markProfileSynced(profile: Profile) {
        database.withTransaction {
            val current = dao.profile() ?: return@withTransaction
            if (current.toProfile() == profile) dao.upsertProfile(current.copy(pendingSync = false))
        }
    }

    /**
     * Borra todo lo que pertenece al usuario que cierra sesión: perfil, guardados, cola de salida, búsquedas
     * recientes y la marca de última sincronización. Los datos públicos (restaurantes,
     * catálogo, reseñas, ocupación) se conservan para que la siguiente sesión también funcione sin conexión.
     */
    suspend fun clearUserData() {
        database.withTransaction {
            dao.clearProfile()
            dao.clearSaved()
            dao.clearOutbox()
            dao.clearRecentSearches()
            dao.deleteDocument(LAST_SYNC_KEY)
            dao.deleteDocument(DATA_OWNER_KEY)
            dao.deleteDocument(RECENT_VIEWS_KEY)
        }
    }

    /** Solo el cache de lectura (documentos); no toca datos del usuario ni la cola de salida. */
    suspend fun clearCachedDocuments() {
        database.withTransaction {
            dao.deleteDocumentsExcept(listOf(DATA_OWNER_KEY, LAST_SYNC_KEY, RECENT_VIEWS_KEY))
            dao.clearRankingSnapshots()
        }
    }

    suspend fun saveRankingSnapshot(
        criterion: RankingCriterion,
        window: RankingWindow,
        rows: List<SnapshotRow>,
        computedAt: Long
    ) {
        val entities = rows.mapIndexed { index, row ->
            RankingSnapshotEntity(
                criterionName = criterion.name,
                windowName = window.name,
                generation = CURRENT_RANKING,
                position = index + 1,
                restaurantId = row.restaurantId,
                score = row.score,
                reviewCount = row.reviewCount,
                computedAt = computedAt
            )
        }
        database.withTransaction {
            dao.deleteRankingGeneration(criterion.name, window.name, PREVIOUS_RANKING)
            dao.moveRankingGeneration(criterion.name, window.name, CURRENT_RANKING, PREVIOUS_RANKING)
            dao.insertRankingSnapshot(entities)
        }
    }

    suspend fun loadRankingSnapshot(
        criterion: RankingCriterion,
        window: RankingWindow,
        generation: Int = CURRENT_RANKING
    ): StoredRanking? {
        val rows = dao.rankingSnapshot(criterion.name, window.name, generation)
        if (rows.isEmpty()) return null
        return StoredRanking(
            rows = rows.map { SnapshotRow(it.restaurantId, it.score, it.reviewCount) },
            computedAt = rows.first().computedAt
        )
    }

    suspend fun documentCount(): Int = dao.documentCount()

    suspend fun documentBytes(): Long = dao.documentBytes()

    suspend fun pendingCountExcludingAnalytics(): Int = dao.outboxCountExcluding(OutboxKind.ANALYTICS)

    override suspend fun enqueue(request: OutboxRequest) {
        database.withTransaction {
            dao.insertOutbox(
                OutboxEntity(
                    kind = request.kind,
                    method = request.method,
                    path = request.path,
                    body = OutboxCodec.encode(request.body),
                    createdAt = clock(),
                    requestId = request.requestId
                )
            )
            dao.trimOutbox(MAX_OUTBOX_ENTRIES)
        }
    }

    override suspend fun pending(): List<OutboxEntry> = dao.outbox().map {
        val requestId = it.requestId.ifBlank { "legacy-${it.id}" }
        OutboxEntry(
            it.id,
            OutboxRequest(it.kind, it.method, it.path, OutboxCodec.decode(it.body), requestId),
            it.attempts,
            it.nextAttemptAt
        )
    }

    override suspend fun remove(id: Long) = dao.deleteOutbox(id)

    override suspend fun recordAttempt(id: Long) = dao.bumpOutboxAttempts(id)

    override suspend fun scheduleRetry(id: Long, notBefore: Long) = dao.setOutboxRetryAt(id, notBefore)

    override suspend fun count(): Int = dao.outboxCount()

    override suspend fun clear() = dao.clearOutbox()

    companion object {
        const val MAX_OUTBOX_ENTRIES = 1000
        /** Documento que guarda el id del usuario dueño de los datos locales (evita mezclar cuentas). */
        const val DATA_OWNER_KEY = "data_owner"
        const val LAST_SYNC_KEY = "last_sync"
        const val RECENT_VIEWS_KEY = "recent_views"
        const val CURRENT_RANKING = 0
        const val PREVIOUS_RANKING = 1
    }

    suspend fun loadDocument(key: String): CacheDocumentEntity? = dao.document(key)

    suspend fun deleteDocument(key: String) {
        dao.deleteDocument(key)
    }

    suspend fun saveDocument(key: String, payload: String, fetchedAt: Long) {
        dao.upsertDocument(CacheDocumentEntity(key, payload, fetchedAt))
    }
}

private fun Review.toEntity(position: Int) = ReviewEntity(
    id = id,
    position = position,
    restaurantId = restaurantId,
    author = author,
    date = date,
    taste = ratings.taste,
    attention = ratings.attention,
    price = ratings.price,
    options = ratings.options,
    dietaryOptions = StringListCodec.encode(dietaryOptions),
    comment = comment
)

private fun ReviewEntity.toReview() = Review(
    id = id,
    restaurantId = restaurantId,
    author = author,
    date = date,
    ratings = ReviewRatings(taste = taste, attention = attention, price = price, options = options),
    dietaryOptions = StringListCodec.decode(dietaryOptions),
    comment = comment
)

private fun Profile.toEntity() = ProfileEntity(
    name = name,
    frequency = frequency,
    budget = budget,
    dietaryRestrictions = StringListCodec.encode(dietaryRestrictions),
    cuisinePreferences = StringListCodec.encode(cuisinePreferences),
    usualMealTimes = StringListCodec.encode(usualMealTimes),
    topPriorities = StringListCodec.encode(topPriorities),
    foodsToAvoid = StringListCodec.encode(foodsToAvoid)
)

private fun ProfileEntity.toProfile() = Profile(
    name = name,
    frequency = frequency,
    budget = budget,
    dietaryRestrictions = StringListCodec.decode(dietaryRestrictions),
    cuisinePreferences = StringListCodec.decode(cuisinePreferences),
    usualMealTimes = StringListCodec.decode(usualMealTimes),
    topPriorities = StringListCodec.decode(topPriorities),
    foodsToAvoid = StringListCodec.decode(foodsToAvoid)
)
