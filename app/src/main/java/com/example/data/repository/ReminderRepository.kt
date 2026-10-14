package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.ReminderDao
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReminderRepository(
    private val dao: ReminderDao,
    private val context: Context
) {
    val allReminders: Flow<List<ReminderItem>> = dao.getAllReminders()

    suspend fun getReminderById(id: Long): ReminderItem? {
        return dao.getReminderById(id)
    }

    suspend fun saveReminder(reminder: ReminderItem): Long {
        val id = if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }

        val updated = reminder.copy(id = id)
        if (updated.isEnabled) {
            AlarmScheduler.scheduleReminder(context, updated)
        } else {
            AlarmScheduler.cancelReminder(context, id)
        }
        return id
    }

    suspend fun deleteReminder(reminder: ReminderItem) {
        AlarmScheduler.cancelReminder(context, reminder.id)
        dao.delete(reminder)
    }

    suspend fun setReminderEnabled(id: Long, isEnabled: Boolean) {
        dao.updateEnabled(id, isEnabled)
        val reminder = dao.getReminderById(id) ?: return
        if (isEnabled) {
            AlarmScheduler.scheduleReminder(context, reminder)
        } else {
            AlarmScheduler.cancelReminder(context, id)
        }
    }

    suspend fun rescheduleAllEnabledReminders() {
        val enabledList = dao.getEnabledRemindersSync()
        for (item in enabledList) {
            AlarmScheduler.scheduleReminder(context, item)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = dao.getEnabledRemindersSync()
        if (existing.isEmpty()) {
            val now = Calendar.getInstance()
            val sampleHour1 = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
            val sampleHour2 = (now.get(Calendar.HOUR_OF_DAY) + 3) % 24

            val sample1 = ReminderItem(
                title = "مراجعة المهام اليومية والرياضة",
                description = "جلسة نشاط واستماع لموسيقى محفزة",
                category = "health",
                timeHour = sampleHour1,
                timeMinute = 0,
                isDaily = true,
                isEnabled = true,
                songType = "builtin",
                songTitle = "نبض الحماس (Energy Rhythm)",
                songUriOrId = "preset_energy",
                vibrate = true,
                volume = 1.0f
            )

            val sample2 = ReminderItem(
                title = "اجتماع العمل والمتابعة",
                description = "مناقشة خطة المشروع مع الفريق",
                category = "work",
                timeHour = sampleHour2,
                timeMinute = 30,
                isDaily = true,
                isEnabled = true,
                songType = "builtin",
                songTitle = "أمل الصباح (Morning Hope)",
                songUriOrId = "preset_morning",
                vibrate = true,
                volume = 1.0f
            )

            saveReminder(sample1)
            saveReminder(sample2)
        }
    }
}
