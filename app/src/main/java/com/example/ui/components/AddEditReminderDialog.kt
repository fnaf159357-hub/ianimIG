package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.audio.MelodyOption
import com.example.data.model.ReminderItem
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.PurplePrimary
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditReminderDialog(
    initialReminder: ReminderItem?,
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit,
    playingPreviewId: String?,
    onPlayPreview: (MelodyOption) -> Unit
) {
    val isEdit = initialReminder != null

    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialReminder?.category ?: "general") }

    // Time states
    val currentCal = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(initialReminder?.timeHour ?: (currentCal.get(Calendar.HOUR_OF_DAY) + 1) % 24) }
    var minute by remember { mutableIntStateOf(initialReminder?.timeMinute ?: 0) }

    // Repeat mode: "daily", "custom_days", "once"
    var repeatMode by remember {
        mutableStateOf(
            when {
                initialReminder == null -> "daily"
                initialReminder.isDaily -> "daily"
                initialReminder.repeatDays.isNotEmpty() -> "custom_days"
                else -> "once"
            }
        )
    }

    var selectedDays by remember {
        mutableStateOf(initialReminder?.repeatDays ?: listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY))
    }

    // Song states
    var songType by remember { mutableStateOf(initialReminder?.songType ?: "builtin") }
    var songTitle by remember { mutableStateOf(initialReminder?.songTitle ?: "أمل الصباح (Morning Hope)") }
    var songUriOrId by remember { mutableStateOf(initialReminder?.songUriOrId ?: "preset_morning") }

    var vibrate by remember { mutableStateOf(initialReminder?.vibrate ?: true) }
    var volume by remember { mutableFloatStateOf(initialReminder?.volume ?: 1.0f) }

    val daysOfWeek = listOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("add_edit_reminder_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) stringResource(R.string.edit_reminder) else stringResource(R.string.add_reminder),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reminder_title)) },
                    placeholder = { Text(stringResource(R.string.reminder_title_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Title Suggestions Chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("طبيب الأسنان", "مذاكرة", "اجتماع عمل", "دواء الصباح", "صلاة وتدبر", "تمرين رياضي").forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { title = suggestion }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ $suggestion",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.reminder_notes)) },
                    placeholder = { Text(stringResource(R.string.reminder_notes_hint)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "🏷️ التصنيف:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryData.list.forEach { cat ->
                        val isSelected = selectedCategory == cat.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.key },
                            label = { Text(cat.title, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Picker Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وقت رنين الموعد:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Large formatted time display
                        val amPm = if (hour >= 12) "م" else "ص"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        val formattedTime = String.format("%02d:%02d %s", displayHour, minute, amPm)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, PurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PurplePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hour slider
                        Text("الساعة: $displayHour $amPm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(thumbColor = PurplePrimary, activeTrackColor = PurplePrimary)
                        )

                        // Minute slider
                        Text("الدقيقة: $minute", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = minute.toFloat(),
                            onValueChange = { minute = it.toInt() },
                            valueRange = 0f..59f,
                            steps = 58,
                            colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Mode
                Text(
                    text = "🔁 نظام التكرار:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("daily" to "يومياً", "custom_days" to "أيام محددة", "once" to "مرة واحدة")
                    modes.forEach { (key, label) ->
                        val isSelected = repeatMode == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { repeatMode = key }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // If custom days chosen, show Day chips
                if (repeatMode == "custom_days") {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysOfWeek.forEach { (calDay, dayName) ->
                            val isDaySelected = selectedDays.contains(calDay)
                            FilterChip(
                                selected = isDaySelected,
                                onClick = {
                                    selectedDays = if (isDaySelected) {
                                        selectedDays.filter { it != calDay }
                                    } else {
                                        selectedDays + calDay
                                    }
                                },
                                label = { Text(dayName, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Song Picker Section
                SongPickerSection(
                    selectedSongType = songType,
                    selectedSongTitle = songTitle,
                    selectedSongUriOrId = songUriOrId,
                    onSongSelected = { type, sTitle, uri ->
                        songType = type
                        songTitle = sTitle
                        songUriOrId = uri
                    },
                    playingPreviewId = playingPreviewId,
                    onPlayPreview = onPlayPreview
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vibration & Volume
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.vibrate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = vibrate,
                                onCheckedChange = { vibrate = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = PurplePrimary)
                            Text(text = "مستوى صوت الأغنية:", fontSize = 12.sp)
                            Slider(
                                value = volume,
                                onValueChange = { volume = it },
                                valueRange = 0.2f..1.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons (Save & Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = {
                            val finalTitle = title.ifBlank { "موعد خاص" }
                            val isDailyMode = repeatMode == "daily"
                            val finalDays = if (repeatMode == "custom_days") selectedDays else emptyList()

                            val reminderToSave = ReminderItem(
                                id = initialReminder?.id ?: 0L,
                                title = finalTitle,
                                description = description,
                                category = selectedCategory,
                                timeHour = hour,
                                timeMinute = minute,
                                dateEpochDay = if (repeatMode == "once") System.currentTimeMillis() / 86400000L else null,
                                repeatDays = finalDays,
                                isDaily = isDailyMode,
                                isEnabled = initialReminder?.isEnabled ?: true,
                                songType = songType,
                                songTitle = songTitle,
                                songUriOrId = songUriOrId,
                                vibrate = vibrate,
                                volume = volume
                            )
                            onSave(reminderToSave)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_reminder_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
