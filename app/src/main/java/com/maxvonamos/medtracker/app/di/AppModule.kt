package com.maxvonamos.medtracker.app.di

import android.content.Context
import androidx.room.Room
import com.maxvonamos.medtracker.app.data.dao.MedicationDao
import com.maxvonamos.medtracker.app.data.database.MedTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MedTrackerDatabase =
        Room.databaseBuilder(
            context,
            MedTrackerDatabase::class.java,
            "medtracker.db"
        ).build()

    @Provides
    fun provideDao(database: MedTrackerDatabase): MedicationDao =
        database.medicationDao()
}
