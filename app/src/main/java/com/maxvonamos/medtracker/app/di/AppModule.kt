package com.maxvonamos.medtracker.app.di

import android.content.Context
import androidx.room.Room
import com.maxvonamos.medtracker.app.data.dao.MedicationDao
import com.maxvonamos.medtracker.app.data.dao.ReminderDao
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
        )
            .addMigrations(
                MedTrackerDatabase.MIGRATION_1_2,
                MedTrackerDatabase.MIGRATION_2_3
            )
            .build()

    @Provides
    fun provideDao(database: MedTrackerDatabase): MedicationDao =
        database.medicationDao()

    @Provides
    fun provideReminderDao(database: MedTrackerDatabase): ReminderDao =
        database.reminderDao()
}
