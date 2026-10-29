package com.uniandes.campuseats.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReviewEntity::class,
        CrowdingReportEntity::class,
        SavedRestaurantEntity::class,
        RecentSearchEntity::class,
        ProfileEntity::class,
        CacheDocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): AppDao

    companion object {
        private const val NAME = "campus_eats_local.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
