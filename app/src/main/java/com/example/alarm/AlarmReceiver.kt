package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.example.ACTION_DISMISS_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "melodyreminder:alarm_receiver_wakelock"
                )
                wakeLock?.acquire(30000L) // 30 seconds safety max

                val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_TITLE) ?: "تذكير موعدك"
                val desc = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_DESC) ?: ""
                val songType = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TYPE) ?: "builtin"
                val songTitle = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TITLE) ?: "أمل الصباح"
                val songUri = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_URI) ?: "preset_morning"
                val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
                val volume = intent.getFloatExtra(AlarmScheduler.EXTRA_VOLUME, 1.0f)

                // 1. Start Audio Foreground Service to start playing the song
                val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
                    putExtra(AlarmScheduler.EXTRA_REMINDER_TITLE, title)
                    putExtra(AlarmScheduler.EXTRA_REMINDER_DESC, desc)
                    putExtra(AlarmScheduler.EXTRA_SONG_TYPE, songType)
                    putExtra(AlarmScheduler.EXTRA_SONG_TITLE, songTitle)
                    putExtra(AlarmScheduler.EXTRA_SONG_URI, songUri)
                    putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
                    putExtra(AlarmScheduler.EXTRA_VOLUME, volume)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // 2. Open Ringing Full-screen Activity
                val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
                    putExtra(AlarmScheduler.EXTRA_REMINDER_TITLE, title)
                    putExtra(AlarmScheduler.EXTRA_REMINDER_DESC, desc)
                    putExtra(AlarmScheduler.EXTRA_SONG_TYPE, songType)
                    putExtra(AlarmScheduler.EXTRA_SONG_TITLE, songTitle)
                    putExtra(AlarmScheduler.EXTRA_SONG_URI, songUri)
                }
                context.startActivity(activityIntent)

                wakeLock?.release()
            }

            ACTION_DISMISS_REMINDER -> {
                val serviceIntent = Intent(context, AlarmAudioService::class.java)
                context.stopService(serviceIntent)
            }

            ACTION_SNOOZE_REMINDER -> {
                val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_TITLE) ?: "الموعد"
                val songType = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TYPE) ?: "builtin"
                val songTitle = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TITLE) ?: "أمل الصباح"
                val songUri = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_URI) ?: "preset_morning"

                // Stop active playing
                val serviceIntent = Intent(context, AlarmAudioService::class.java)
                context.stopService(serviceIntent)

                // Schedule snooze alarm for 5 mins
                AlarmScheduler.scheduleSnooze(context, reminderId, title, songType, songTitle, songUri, 5)
            }
        }
    }
}
