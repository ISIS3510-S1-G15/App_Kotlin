package com.uniandes.campuseats.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReviewEntity::class,
        CrowdingReportEntity::class,
        SavedRestaurantEntity::class,
        RecentSearchEntity::class,
        ProfileEntity::class,
        CacheDocumentEntity::class,
        OutboxEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): AppDao

    companion object {
        private const val NAME = "campus_eats_local.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `outbox` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "`kind` TEXT NOT NULL, `method` TEXT NOT NULL, `path` TEXT NOT NULL, `body` TEXT, "
                        + "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL)"
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
