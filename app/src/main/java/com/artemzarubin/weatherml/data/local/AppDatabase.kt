package com.artemzarubin.weatherml.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLocationEntity::class],
    version = 1, // Initial version
    exportSchema = false // Can be set to true to export the schema
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao

    // You can add a companion object for the database name if needed.
    // companion object {
    //     const val DATABASE_NAME = "weather_ml_app_database"
    // }
}