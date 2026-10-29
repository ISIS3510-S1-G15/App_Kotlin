package com.uniandes.campuseats.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val restaurantId: String,
    val author: String,
    val date: String,
    val taste: Int,
    val attention: Int,
    val price: Int,
    val options: Int,
    val dietaryOptions: String,
    val comment: String
)

@Entity(tableName = "crowding_reports")
data class CrowdingReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: String,
    val level: Int
)

@Entity(tableName = "saved_restaurants")
data class SavedRestaurantEntity(@PrimaryKey val restaurantId: String)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(@PrimaryKey val position: Int, val name: String)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLE_PROFILE_ID,
    val name: String,
    val frequency: String,
    val budget: String,
    val dietaryRestrictions: String,
    val cuisinePreferences: String,
    val usualMealTimes: String,
    val topPriorities: String,
    val foodsToAvoid: String,
    val pendingSync: Boolean = false
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}

@Entity(tableName = "cache_documents")
data class CacheDocumentEntity(
    @PrimaryKey val key: String,
    val payload: String,
    val fetchedAt: Long
)
