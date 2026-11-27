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
        OutboxEntity::class,
        RankingSnapshotEntity::class,
        RecentViewEntity::class
    ],
    version = 5,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `requestId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `nextAttemptAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ranking_snapshots` (`criterionName` TEXT NOT NULL, "
                        + "`windowName` TEXT NOT NULL, `generation` INTEGER NOT NULL, `position` INTEGER NOT NULL, "
                        + "`restaurantId` TEXT NOT NULL, `score` REAL NOT NULL, `reviewCount` INTEGER NOT NULL, "
                        + "`computedAt` INTEGER NOT NULL, "
                        + "PRIMARY KEY(`criterionName`, `windowName`, `generation`, `position`))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_views` (`restaurantId` TEXT NOT NULL, "
                        + "`viewCount` INTEGER NOT NULL, `lastViewedAt` INTEGER NOT NULL, PRIMARY KEY(`restaurantId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_views_lastViewedAt` ON `recent_views` (`lastViewedAt`)")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
