package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MelodyManager
import com.example.audio.MelodyOption
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurplePrimary

@Composable
fun SongPickerSection(
    selectedSongType: String,
    selectedSongTitle: String,
    selectedSongUriOrId: String,
    onSongSelected: (songType: String, songTitle: String, uriOrId: String) -> Unit,
    playingPreviewId: String?,
    onPlayPreview: (MelodyOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var systemRingtones by remember { mutableStateOf<List<MelodyOption>>(emptyList()) }
    var customSongSelected by remember {
        mutableStateOf(
            if (selectedSongType == "custom" || selectedSongType == "custom_file") {
                MelodyOption(
                    id = selectedSongUriOrId,
                    title = selectedSongTitle,
                    category = "custom",
                    uriOrPath = selectedSongUriOrId,
                    description = "أغنيتك الخاصة المختارة"
                )
            } else null
        )
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedOption = MelodyManager.saveCustomSong(context, it)
            if (savedOption != null) {
                customSongSelected = savedOption
                onSongSelected("custom_file", savedOption.title, savedOption.uriOrPath)
                onPlayPreview(savedOption)
            }
        }
    }

    LaunchedEffect(Unit) {
        systemRingtones = MelodyManager.getSystemRingtones(context)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "🎵 اختر الأغنية أو النغمة للتذكير:",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Current selection preview banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("current_song_preview_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PurplePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "النغمة المعتمدة للموعد:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedSongTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quick Play/Stop Preview
                val isCurrentPlaying = playingPreviewId == selectedSongUriOrId ||
                        (selectedSongUriOrId.startsWith("builtin:") && playingPreviewId == selectedSongUriOrId.removePrefix("builtin:"))

                IconButton(
                    onClick = {
                        val option = MelodyOption(
                            id = selectedSongUriOrId,
                            title = selectedSongTitle,
                            category = selectedSongType,
                            uriOrPath = selectedSongUriOrId
                        )
                        onPlayPreview(option)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isCurrentPlaying) AmberAccent else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentPlaying) "إيقاف" else "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Tabs: 0: نغمات جاهزة | 1: أغنية من هاتفك | 2: نغمات النظام
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .padding(2.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("ألحان جاهزة", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("أغنيتك من الهاتف", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("نغمات النظام", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Builtin melodies list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MelodyManager.BUILTIN_MELODIES.forEach { melody ->
                        val isSelected = selectedSongUriOrId == melody.uriOrPath || selectedSongUriOrId == melody.id
                        val isPlaying = playingPreviewId == melody.id

                        MelodyRowItem(
                            melody = melody,
                            isSelected = isSelected,
                            isPlaying = isPlaying,
                            onSelect = {
                                onSongSelected("builtin", melody.title, melody.uriOrPath)
                            },
                            onPlay = {
                                onPlayPreview(melody)
                            }
                        )
                    }
                }
            }
            1 -> {
                // Pick custom song from device
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "يمكنك اختيار أي أغنية أو ملف صوتي (MP3, M4A, WAV) محفوظ على هاتفك ليكون نغمة موعدك الخاصة!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_device_audio_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصفح واختيار أغنية من الهاتف",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }

                    customSongSelected?.let { song ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val isPlaying = playingPreviewId == song.id
                        MelodyRowItem(
                            melody = song,
                            isSelected = selectedSongType.contains("custom"),
                            isPlaying = isPlaying,
                            onSelect = {
                                onSongSelected("custom_file", song.title, song.uriOrPath)
                            },
                            onPlay = {
                                onPlayPreview(song)
                            }
                        )
                    }
                }
            }
            2 -> {
                // System Ringtones
                if (systemRingtones.isEmpty()) {
                    Text(
                        text = "جاري تحميل نغمات المنبه من الهاتف...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        systemRingtones.take(6).forEach { melody ->
                            val isSelected = selectedSongUriOrId == melody.uriOrPath
                            val isPlaying = playingPreviewId == melody.id

                            MelodyRowItem(
                                melody = melody,
                                isSelected = isSelected,
                                isPlaying = isPlaying,
                                onSelect = {
                                    onSongSelected("system_ringtone", melody.title, melody.uriOrPath)
                                },
                                onPlay = {
                                    onPlayPreview(melody)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MelodyRowItem(
    melody: MelodyOption,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, PurplePrimary, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .testTag("melody_item_${melody.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio icon
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = melody.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (melody.description.isNotBlank()) {
                    Text(
                        text = melody.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Play / Pause Preview button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isPlaying) AmberAccent else MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف" else "استماع",
                    tint = if (isPlaying) Color.White else PurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
