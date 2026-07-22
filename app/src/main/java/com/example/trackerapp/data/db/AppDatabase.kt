package com.example.trackerapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventTypeEntity::class, EventEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventEntryDao(): EventEntryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trackerapp.db"
                )
                    // No exported schema history yet and nothing production-critical
                    // stored on-device — destructive migration is fine until the
                    // first real Migration is worth writing.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
