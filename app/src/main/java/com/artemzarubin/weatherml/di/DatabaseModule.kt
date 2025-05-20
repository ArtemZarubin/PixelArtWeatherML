package com.artemzarubin.weatherml.di

import android.content.Context
import androidx.room.Room
import com.artemzarubin.weatherml.data.local.AppDatabase
import com.artemzarubin.weatherml.data.local.SavedLocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class) // Dependencies will live as long as the Application lives.
object DatabaseModule {

    @Provides
    @Singleton // Guarantees that there will be only one instance of AppDatabase
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "weather_locations.db" // The name of the database file
        )
            // .fallbackToDestructiveMigration() // For development, if you don't want to write migrations
            .build()
    }

    @Provides
    @Singleton // com.artemzarubin.weatherml.data.localDAO can also be a singleton
    fun provideSavedLocationDao(database: AppDatabase): SavedLocationDao {
        return database.savedLocationDao()
    }
}