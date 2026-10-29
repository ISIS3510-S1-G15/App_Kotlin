package com.uniandes.campuseats.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun profile(id: Int = ProfileEntity.SINGLE_PROFILE_ID): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Query("SELECT * FROM cache_documents WHERE `key` = :key")
    suspend fun document(key: String): CacheDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: CacheDocumentEntity)
}
