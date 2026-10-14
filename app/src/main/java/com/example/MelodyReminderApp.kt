package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import com.example.audio.MelodyManager
import com.example.data.local.AppDatabase
import com.example.data.repository.ReminderRepository

class MelodyReminderApp : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "melody_reminder_channel_high"
        lateinit var instance: MelodyReminderApp
            private set
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ReminderRepository(database.reminderDao(), this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        // Ensure default built-in melodies are prepared in background
        MelodyManager.prepareBuiltinMelodies(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(null, null) // Sound is handled directly by AlarmAudioService for exact custom song playback
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
