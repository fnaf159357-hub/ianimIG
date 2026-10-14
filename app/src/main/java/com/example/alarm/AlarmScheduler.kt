package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.ReminderItem
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    const val EXTRA_REMINDER_DESC = "extra_reminder_desc"
    const val EXTRA_SONG_TYPE = "extra_song_type"
    const val EXTRA_SONG_TITLE = "extra_song_title"
    const val EXTRA_SONG_URI = "extra_song_uri"
    const val EXTRA_VIBRATE = "extra_vibrate"
    const val EXTRA_VOLUME = "extra_volume"

    fun scheduleReminder(context: Context, reminder: ReminderItem) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder.id)
            return
        }

        val triggerTime = calculateNextTriggerTime(reminder)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time is in the past for reminder ${reminder.id}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_DESC, reminder.description)
            putExtra(EXTRA_SONG_TYPE, reminder.songType)
            putExtra(EXTRA_SONG_TITLE, reminder.songTitle)
            putExtra(EXTRA_SONG_URI, reminder.songUriOrId)
            putExtra(EXTRA_VIBRATE, reminder.vibrate)
            putExtra(EXTRA_VOLUME, reminder.volume)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Check canScheduleExactAlarms on Android 12+
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            // An intent to open MainActivity when user clicks the alarm clock icon on lockscreen
            val showIntent = Intent(context, MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Scheduled alarm clock for ${reminder.title} at $triggerTime")
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "Scheduled inexact/allowWhileIdle alarm for ${reminder.title} at $triggerTime")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback alarm schedule also failed: ${ex.message}")
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder $reminderId")
        }
    }

    fun scheduleSnooze(context: Context, reminderId: Long, title: String, songType: String, songTitle: String, songUri: String, minutes: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, "$title (غفوة)")
            putExtra(EXTRA_SONG_TYPE, songType)
            putExtra(EXTRA_SONG_TITLE, songTitle)
            putExtra(EXTRA_SONG_URI, songUri)
            putExtra(EXTRA_VIBRATE, true)
            putExtra(EXTRA_VOLUME, 1.0f)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 99999).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d(TAG, "Snoozed alarm for $title in $minutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to snooze alarm: ${e.message}")
        }
    }

    fun calculateNextTriggerTime(reminder: ReminderItem): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.timeHour)
            set(Calendar.MINUTE, reminder.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If one-time with specific date
        if (!reminder.isDaily && reminder.repeatDays.isEmpty() && reminder.dateEpochDay != null) {
            val epochDayCal = Calendar.getInstance().apply {
                timeInMillis = reminder.dateEpochDay * 86400000L
                set(Calendar.HOUR_OF_DAY, reminder.timeHour)
                set(Calendar.MINUTE, reminder.timeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return epochDayCal.timeInMillis
        }

        // If daily
        if (reminder.isDaily || reminder.repeatDays.isEmpty()) {
            if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // If specific days of week (repeatDays contains Calendar.SUNDAY..SATURDAY: 1..7)
        var minTriggerTime = Long.MAX_VALUE
        for (dayOfWeek in reminder.repeatDays) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, reminder.timeHour)
                set(Calendar.MINUTE, reminder.timeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
            }
            if (dayCal.before(now) || dayCal.timeInMillis <= now.timeInMillis) {
                dayCal.add(Calendar.WEEK_OF_YEAR, 1)
            }
            if (dayCal.timeInMillis < minTriggerTime) {
                minTriggerTime = dayCal.timeInMillis
            }
        }

        return if (minTriggerTime != Long.MAX_VALUE) minTriggerTime else target.timeInMillis
    }
}
