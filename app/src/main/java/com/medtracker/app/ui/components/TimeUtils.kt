package com.medtracker.app.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$mins min${if (mins > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hr${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(6) -> {
            // Show day of week + time for entries less than 6 days old
            val sdf = SimpleDateFormat("EEEE 'at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val sdf = SimpleDateFormat("'Today at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < TimeUnit.DAYS.toMillis(2) -> {
            val sdf = SimpleDateFormat("'Yesterday at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < TimeUnit.DAYS.toMillis(6) -> {
            val sdf = SimpleDateFormat("EEEE 'at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    // Check if same calendar day
    val todayCal = Calendar.getInstance().apply { timeInMillis = now }
    val tsCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = todayCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == tsCal.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> "Today"
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(6) -> {
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
