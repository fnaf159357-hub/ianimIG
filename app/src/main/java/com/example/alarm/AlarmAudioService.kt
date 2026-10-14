package com.example.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MelodyReminderApp
import com.example.R
import com.example.audio.MelodyManager
import java.io.File

class AlarmAudioService : Service() {

    companion object {
        private const val TAG = "AlarmAudioService"
        private const val NOTIFICATION_ID = 4040
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_TITLE) ?: "تذكير موعدك"
        val desc = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_DESC) ?: ""
        val songType = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TYPE) ?: "builtin"
        val songTitle = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_TITLE) ?: "أمل الصباح"
        val songUri = intent.getStringExtra(AlarmScheduler.EXTRA_SONG_URI) ?: "preset_morning"
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        val volume = intent.getFloatExtra(AlarmScheduler.EXTRA_VOLUME, 1.0f)

        // 1. Build notification with Fullscreen Intent & Actions
        val notification = buildForegroundNotification(reminderId, title, desc, songType, songTitle, songUri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. Play audio
        playSong(songType, songUri, volume)

        // 3. Start Vibration
        if (vibrate) {
            startVibration()
        }

        return START_STICKY
    }

    private fun playSong(songType: String, songUri: String, volume: Float) {
        stopAudio()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }

            when {
                songType == "builtin" || songUri.startsWith("builtin:") -> {
                    val presetId = songUri.removePrefix("builtin:")
                    val file = MelodyManager.getMelodyFile(this, presetId)
                    player.setDataSource(file.absolutePath)
                }
                songType == "custom" || songType == "custom_file" -> {
                    val file = File(songUri)
                    if (file.exists()) {
                        player.setDataSource(file.absolutePath)
                    } else {
                        player.setDataSource(this, Uri.parse(songUri))
                    }
                }
                else -> {
                    // System ringtone or general URI
                    player.setDataSource(this, Uri.parse(songUri))
                }
            }

            player.isLooping = true
            val clampedVol = volume.coerceIn(0.1f, 1.0f)
            player.setVolume(clampedVol, clampedVol)
            player.prepare()
            player.start()
            mediaPlayer = player
            Log.d(TAG, "Playing song: $songUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song: ${e.message}", e)
            // Fallback to default preset if custom failed
            try {
                val fallbackFile = MelodyManager.getMelodyFile(this, "preset_morning")
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setDataSource(fallbackFile.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = player
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback also failed: ${ex.message}")
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun buildForegroundNotification(
        reminderId: Long,
        title: String,
        desc: String,
        songType: String,
        songTitle: String,
        songUri: String
    ): Notification {
        // Fullscreen Intent to open AlarmRingingActivity
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_REMINDER_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_REMINDER_DESC, desc)
            putExtra(AlarmScheduler.EXTRA_SONG_TYPE, songType)
            putExtra(AlarmScheduler.EXTRA_SONG_TITLE, songTitle)
            putExtra(AlarmScheduler.EXTRA_SONG_URI, songUri)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminderId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action Intent
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_REMINDER
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (reminderId + 1000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action Intent
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_REMINDER
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_REMINDER_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_SONG_TYPE, songType)
            putExtra(AlarmScheduler.EXTRA_SONG_TITLE, songTitle)
            putExtra(AlarmScheduler.EXTRA_SONG_URI, songUri)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            (reminderId + 2000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (desc.isNotBlank()) "$desc • الأغنية: $songTitle" else "الأغنية: $songTitle"

        return NotificationCompat.Builder(this, MelodyReminderApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $title")
            .setContentText(subtext)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "🛑 إيقاف التذكير", dismissPendingIntent)
            .addAction(0, "💤 غفوة (5د)", snoozePendingIntent)
            .build()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator: ${e.message}")
        } finally {
            vibrator = null
        }
    }

    override fun onDestroy() {
        stopAudio()
        stopVibration()
        super.onDestroy()
    }
}
