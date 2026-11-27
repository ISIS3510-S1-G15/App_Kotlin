package com.uniandes.campuseats.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM reviews ORDER BY position")
    suspend fun reviews(): List<ReviewEntity>

    @Query("DELETE FROM reviews")
    suspend fun clearReviews()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(items: List<ReviewEntity>)

    @Query("SELECT * FROM crowding_reports ORDER BY id")
    suspend fun crowdingReports(): List<CrowdingReportEntity>

    @Query("DELETE FROM crowding_reports")
    suspend fun clearCrowding()

    @Insert
    suspend fun insertCrowding(items: List<CrowdingReportEntity>)

    @Query("SELECT * FROM saved_restaurants")
    suspend fun savedRestaurants(): List<SavedRestaurantEntity>

    @Query("DELETE FROM saved_restaurants")
    suspend fun clearSaved()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaved(items: List<SavedRestaurantEntity>)

    @Query("SELECT * FROM recent_searches ORDER BY position")
    suspend fun recentSearches(): List<RecentSearchEntity>

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearches(items: List<RecentSearchEntity>)

    @Query("SELECT * FROM recent_views ORDER BY lastViewedAt DESC")
    fun observeRecentViews(): Flow<List<RecentViewEntity>>

    @Query("SELECT * FROM recent_views ORDER BY lastViewedAt DESC")
    suspend fun recentViews(): List<RecentViewEntity>

    @Query("SELECT * FROM recent_views WHERE restaurantId = :restaurantId")
    suspend fun recentView(restaurantId: String): RecentViewEntity?

    @Query("SELECT COUNT(*) FROM recent_views")
    suspend fun recentViewCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentView(item: RecentViewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentViews(items: List<RecentViewEntity>)

    @Query("DELETE FROM recent_views WHERE restaurantId = :restaurantId")
    suspend fun deleteRecentView(restaurantId: String)

    @Query("DELETE FROM recent_views")
    suspend fun clearRecentViews()

    @Query("DELETE FROM recent_views WHERE lastViewedAt < :cutoff")
    suspend fun deleteRecentViewsOlderThan(cutoff: Long)

    @Query("DELETE FROM recent_views WHERE restaurantId NOT IN (SELECT restaurantId FROM recent_views ORDER BY lastViewedAt DESC LIMIT :keep)")
    suspend fun trimRecentViews(keep: Int)

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun profile(id: Int = ProfileEntity.SINGLE_PROFILE_ID): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Insert
    suspend fun insertOutbox(entry: OutboxEntity): Long

    @Query("SELECT * FROM outbox ORDER BY id")
    suspend fun outbox(): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteOutbox(id: Long)

    @Query("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpOutboxAttempts(id: Long)

    @Query("UPDATE outbox SET nextAttemptAt = :notBefore WHERE id = :id")
    suspend fun setOutboxRetryAt(id: Long, notBefore: Long)

    @Query("SELECT COUNT(*) FROM outbox WHERE kind != :excludedKind")
    suspend fun outboxCountExcluding(excludedKind: String): Int

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun outboxCount(): Int

    @Query("DELETE FROM outbox")
    suspend fun clearOutbox()

    @Query("DELETE FROM outbox WHERE id NOT IN (SELECT id FROM outbox ORDER BY id DESC LIMIT :keep)")
    suspend fun trimOutbox(keep: Int)

    @Query("DELETE FROM cache_documents WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteDocumentsWithPrefix(prefix: String)

    @Query("SELECT COUNT(*) FROM cache_documents")
    suspend fun documentCount(): Int

    @Query("SELECT COALESCE(SUM(LENGTH(payload)), 0) FROM cache_documents")
    suspend fun documentBytes(): Long

    @Query("DELETE FROM cache_documents WHERE `key` NOT IN (:keep)")
    suspend fun deleteDocumentsExcept(keep: List<String>)

    @Query("SELECT * FROM cache_documents WHERE `key` = :key")
    suspend fun document(key: String): CacheDocumentEntity?

    @Query("DELETE FROM cache_documents WHERE `key` = :key")
    suspend fun deleteDocument(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: CacheDocumentEntity)

    @Query(
        "SELECT * FROM ranking_snapshots WHERE criterionName = :criterion AND windowName = :window " +
            "AND generation = :generation ORDER BY position"
    )
    suspend fun rankingSnapshot(criterion: String, window: String, generation: Int): List<RankingSnapshotEntity>

    @Query("DELETE FROM ranking_snapshots WHERE criterionName = :criterion AND windowName = :window AND generation = :generation")
    suspend fun deleteRankingGeneration(criterion: String, window: String, generation: Int)

    @Query(
        "UPDATE ranking_snapshots SET generation = :to WHERE criterionName = :criterion AND windowName = :window " +
            "AND generation = :from"
    )
    suspend fun moveRankingGeneration(criterion: String, window: String, from: Int, to: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRankingSnapshot(items: List<RankingSnapshotEntity>)

    @Query("DELETE FROM ranking_snapshots")
    suspend fun clearRankingSnapshots()
}
