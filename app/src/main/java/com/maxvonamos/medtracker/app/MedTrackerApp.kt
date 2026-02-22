package com.maxvonamos.medtracker.app

import android.app.Application
import com.maxvonamos.medtracker.app.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
