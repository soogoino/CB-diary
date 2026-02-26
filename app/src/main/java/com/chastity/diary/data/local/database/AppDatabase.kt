package com.chastity.diary.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chastity.diary.data.local.dao.DailyEntryAttributeDao
import com.chastity.diary.data.local.dao.DailyEntryDao
import com.chastity.diary.data.local.entity.Converters
import com.chastity.diary.data.local.entity.DailyEntryAttributeEntity
import com.chastity.diary.data.local.entity.DailyEntryEntity
import com.chastity.diary.util.Constants

/**
 * Main application database
 */
@Database(
    entities = [DailyEntryEntity::class, DailyEntryAttributeEntity::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun dailyEntryAttributeDao(): DailyEntryAttributeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    // Legacy dev versions (1–3) have no migration path; allow destructive reset
                    // for those only. Version 4+ must have proper migrations in Migrations.kt.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .addMigrations(*Migrations.ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Alias for getInstance
        fun getDatabase(context: Context): AppDatabase = getInstance(context)
    }
}
