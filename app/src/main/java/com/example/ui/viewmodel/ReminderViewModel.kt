package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MelodyReminderApp
import com.example.alarm.AlarmAudioService
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmRingingActivity
import com.example.alarm.AlarmScheduler
import com.example.audio.MelodyManager
import com.example.audio.MelodyOption
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReminderUiState(
    val reminders: List<ReminderItem> = emptyList(),
    val filteredReminders: List<ReminderItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedTab: String = "all", // "all", "today", "repeating"
    val isPlayingPreviewId: String? = null,
    val editingReminder: ReminderItem? = null,
    val isAddEditOpen: Boolean = false
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MelodyReminderApp).repository

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedTab = MutableStateFlow("all")
    private val _playingPreviewId = MutableStateFlow<String?>(null)
    private val _editingReminder = MutableStateFlow<ReminderItem?>(null)
    private val _isAddEditOpen = MutableStateFlow(false)

    private val _filteredFlow = combine(
        repository.allReminders,
        _searchQuery,
        _selectedCategory,
        _selectedTab
    ) { reminders, query, category, tab ->
        val todayCalendar = Calendar.getInstance()
        val currentDayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)

        val filtered = reminders.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.songTitle.contains(query, ignoreCase = true)

            val matchesCategory = category == null || item.category == category

            val matchesTab = when (tab) {
                "today" -> {
                    item.isDaily || item.repeatDays.contains(currentDayOfWeek) || (item.dateEpochDay != null)
                }
                "repeating" -> item.isDaily || item.repeatDays.isNotEmpty()
                else -> true
            }

            matchesQuery && matchesCategory && matchesTab
        }
        Pair(reminders, filtered)
    }

    private val _dialogState = combine(
        _playingPreviewId,
        _editingReminder,
        _isAddEditOpen
    ) { previewId, editing, isOpen ->
        Triple(previewId, editing, isOpen)
    }

    val uiState: StateFlow<ReminderUiState> = combine(
        _filteredFlow,
        _searchQuery,
        _selectedCategory,
        _selectedTab,
        _dialogState
    ) { (reminders, filtered), query, category, tab, (previewId, editing, isOpen) ->
        ReminderUiState(
            reminders = reminders,
            filteredReminders = filtered,
            searchQuery = query,
            selectedCategory = category,
            selectedTab = tab,
            isPlayingPreviewId = previewId,
            editingReminder = editing,
            isAddEditOpen = isOpen
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReminderUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSelectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun onSelectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun openAddDialog() {
        _editingReminder.value = null
        _isAddEditOpen.value = true
    }

    fun openEditDialog(reminder: ReminderItem) {
        _editingReminder.value = reminder
        _isAddEditOpen.value = true
    }

    fun closeAddEditDialog() {
        stopPreview()
        _isAddEditOpen.value = false
        _editingReminder.value = null
    }

    fun saveReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            closeAddEditDialog()
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun toggleReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.setReminderEnabled(reminder.id, !reminder.isEnabled)
        }
    }

    fun playPreview(melody: MelodyOption) {
        if (_playingPreviewId.value == melody.id) {
            stopPreview()
        } else {
            _playingPreviewId.value = melody.id
            MelodyManager.playPreview(getApplication(), melody) {
                _playingPreviewId.value = null
            }
        }
    }

    fun stopPreview() {
        MelodyManager.stopPreview()
        _playingPreviewId.value = null
    }

    /**
     * Instantly triggers the alarm screen and song so the user can test the experience right away!
     */
    fun triggerTestAlarm(context: Context, reminder: ReminderItem) {
        val testIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmScheduler.EXTRA_REMINDER_TITLE, "${reminder.title} (تجربة)")
            putExtra(AlarmScheduler.EXTRA_REMINDER_DESC, reminder.description.ifBlank { "تجربة رنين الأغنية للموعد" })
            putExtra(AlarmScheduler.EXTRA_SONG_TYPE, reminder.songType)
            putExtra(AlarmScheduler.EXTRA_SONG_TITLE, reminder.songTitle)
            putExtra(AlarmScheduler.EXTRA_SONG_URI, reminder.songUriOrId)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, reminder.vibrate)
            putExtra(AlarmScheduler.EXTRA_VOLUME, reminder.volume)
        }
        context.sendBroadcast(testIntent)
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }
}
