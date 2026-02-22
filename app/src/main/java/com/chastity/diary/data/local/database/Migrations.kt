package com.chastity.diary.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations.
 *
 * Naming convention: MIGRATION_<from>_<to>
 *
 * History:
 *   1 → 2, 2 → 3, 3 → 4 : Legacy versions during development; handled by
 *                           fallbackToDestructiveMigrationFrom(1, 2, 3) in AppDatabase.
 *
 * From version 4 onwards, proper migrations are required to preserve user data.
 * Template for next migration:
 *
 *   val MIGRATION_4_5 = object : Migration(4, 5) {
 *       override fun migrate(db: SupportSQLiteDatabase) {
 *           db.execSQL("ALTER TABLE daily_entries ADD COLUMN new_column TEXT")
 *       }
 *   }
 *
 * Add new migrations to the ALL_MIGRATIONS list, then reference them in AppDatabase.
 */
object Migrations {

    /** C-1: Add a UNIQUE INDEX on the `date` column of daily_entries.
     *  Converts getByDate() from O(n) full-table-scan to O(log n).
     *  The UNIQUE constraint also prevents accidental duplicate-date rows. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_entries_date " +
                "ON daily_entries(date)"
            )
        }
    }

    /**
     * All migrations from version 4 onwards.
     * Add new migrations here and pass them to Room via .addMigrations(*ALL_MIGRATIONS).
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_4_5,
    )
}
