package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "general", // work, health, study, personal, spiritual, special, general
    val timeHour: Int, // 0..23
    val timeMinute: Int, // 0..59
    val dateEpochDay: Long? = null, // specific date if not recurring
    val repeatDays: List<Int> = emptyList(), // 1=Sunday, 2=Monday, ..., 7=Saturday (Calendar.SUNDAY..SATURDAY)
    val isDaily: Boolean = true,
    val isEnabled: Boolean = true,
    val songType: String = "builtin", // "builtin", "custom_file", "system_ringtone"
    val songTitle: String = "أمل الصباح",
    val songUriOrId: String = "preset_morning",
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)
