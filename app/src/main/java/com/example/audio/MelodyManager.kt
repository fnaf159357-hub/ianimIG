package com.example.audio

import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

data class MelodyOption(
    val id: String,
    val title: String,
    val category: String, // "builtin", "system", "custom"
    val uriOrPath: String,
    val description: String = ""
)

object MelodyManager {
    private const val TAG = "MelodyManager"
    private var previewPlayer: MediaPlayer? = null
    var currentlyPlayingId: String? = null
        private set

    val BUILTIN_MELODIES = listOf(
        MelodyOption("preset_morning", "أمل الصباح (Morning Hope)", "builtin", "builtin:preset_morning", "لحن مشرق مفعم بالتفاؤل والنشاط"),
        MelodyOption("preset_energy", "نبض الحماس (Energy Rhythm)", "builtin", "builtin:preset_energy", "إيقاع حماسي متصاعد يوقظ الحيوية"),
        MelodyOption("preset_peace", "سكون وسلام (Gentle Harmony)", "builtin", "builtin:preset_peace", "نغمات هادئة راقية تبعث على الطمأنينة"),
        MelodyOption("preset_oriental", "أوتار الأندلس (Acoustic Strings)", "builtin", "builtin:preset_oriental", "عزف وتري دافئ مستوحى من الألحان الشرقية"),
        MelodyOption("preset_bells", "أجراس التفاؤل (Joyful Bells)", "builtin", "builtin:preset_bells", "رنين أجراس موسيقية واضحة ومبهجة")
    )

    fun prepareBuiltinMelodies(context: Context) {
        val dir = File(context.filesDir, "builtin_melodies")
        if (!dir.exists()) dir.mkdirs()

        BUILTIN_MELODIES.forEach { melody ->
            val wavFile = File(dir, "${melody.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 1000) {
                generateMelodyWav(melody.id, wavFile)
            }
        }
    }

    fun getMelodyFile(context: Context, id: String): File {
        val dir = File(context.filesDir, "builtin_melodies")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$id.wav")
        if (!file.exists() || file.length() < 1000) {
            generateMelodyWav(id, file)
        }
        return file
    }

    /**
     * Synthesizes pleasing melodic music chords and harmonies as a standard PCM 16-bit WAV file.
     */
    private fun generateMelodyWav(presetId: String, outputFile: File) {
        val sampleRate = 22050
        val durationSeconds = 8.0 // 8 seconds looping melody

        // Define melody notes (frequencies in Hz) and timings
        val notes: List<Pair<Double, DoubleArray>> = when (presetId) {
            "preset_morning" -> listOf(
                // C major uplifting arpeggio: C5, E5, G5, B5, C6 with harmonies
                0.0 to doubleArrayOf(523.25, 659.25),
                0.5 to doubleArrayOf(659.25, 783.99),
                1.0 to doubleArrayOf(783.99, 1046.50),
                1.5 to doubleArrayOf(1046.50, 1318.51),
                2.2 to doubleArrayOf(880.00, 1046.50),
                2.8 to doubleArrayOf(783.99, 987.77),
                3.5 to doubleArrayOf(659.25, 783.99),
                4.2 to doubleArrayOf(587.33, 783.99),
                5.0 to doubleArrayOf(523.25, 659.25, 1046.50),
                6.5 to doubleArrayOf(783.99, 1046.50)
            )
            "preset_energy" -> listOf(
                // Energetic upbeat rhythm
                0.0 to doubleArrayOf(440.0, 880.0),
                0.3 to doubleArrayOf(554.37, 1108.73),
                0.6 to doubleArrayOf(659.25, 1318.51),
                0.9 to doubleArrayOf(880.0, 1760.0),
                1.4 to doubleArrayOf(659.25, 1318.51),
                1.8 to doubleArrayOf(739.99, 1479.98),
                2.3 to doubleArrayOf(880.0, 1760.0),
                3.0 to doubleArrayOf(987.77, 1975.53),
                3.6 to doubleArrayOf(880.0, 1760.0),
                4.5 to doubleArrayOf(659.25, 1318.51),
                5.5 to doubleArrayOf(880.0, 1760.0)
            )
            "preset_oriental" -> listOf(
                // Warm acoustic Hijaz/Bayati motif
                0.0 to doubleArrayOf(293.66, 440.0), // D4 + A4
                0.6 to doubleArrayOf(311.13, 466.16), // Eb4 + Bb4
                1.2 to doubleArrayOf(369.99, 554.37), // F#4 + C#5
                1.8 to doubleArrayOf(392.00, 587.33), // G4 + D5
                2.6 to doubleArrayOf(440.00, 659.25), // A4 + E5
                3.4 to doubleArrayOf(369.99, 554.37),
                4.2 to doubleArrayOf(311.13, 466.16),
                5.2 to doubleArrayOf(293.66, 440.0, 587.33),
                6.6 to doubleArrayOf(440.0, 587.33)
            )
            "preset_peace" -> listOf(
                // Soothing chords
                0.0 to doubleArrayOf(329.63, 392.00, 493.88), // E minor
                1.5 to doubleArrayOf(392.00, 493.88, 587.33), // G major
                3.0 to doubleArrayOf(349.23, 440.00, 523.25), // F major
                4.5 to doubleArrayOf(293.66, 369.99, 440.00), // D major
                6.0 to doubleArrayOf(329.63, 493.88, 659.25)
            )
            else -> listOf(
                // Preset bells / chime
                0.0 to doubleArrayOf(1046.50, 2093.00),
                0.8 to doubleArrayOf(1318.51, 2637.02),
                1.6 to doubleArrayOf(1567.98, 3135.96),
                2.4 to doubleArrayOf(2093.00, 4186.01),
                3.6 to doubleArrayOf(1567.98, 2093.00),
                4.8 to doubleArrayOf(1318.51, 1567.98),
                6.0 to doubleArrayOf(1046.50, 1318.51, 2093.00)
            )
        }

        val totalSamples = (sampleRate * durationSeconds).toInt()
        val pcm = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0

            for ((startTime, freqs) in notes) {
                if (t >= startTime) {
                    val noteTime = t - startTime
                    // Envelope: fast attack, exponential decay
                    val envelope = Math.exp(-noteTime * 1.5)
                    if (envelope > 0.001) {
                        for (f in freqs) {
                            // Primary tone + harmonic shimmer
                            val wave = sin(2.0 * Math.PI * f * noteTime) +
                                    0.35 * sin(2.0 * Math.PI * f * 2.0 * noteTime) +
                                    0.15 * sin(2.0 * Math.PI * f * 3.0 * noteTime)
                            sampleVal += wave * envelope * (1.0 / freqs.size)
                        }
                    }
                }
            }

            // Normalization and soft limiting
            val clamped = (sampleVal * 0.45).coerceIn(-0.95, 0.95)
            pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        writeWavFile(outputFile, pcm, sampleRate)
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = (pcmData.size * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate.toInt())
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                byteBuffer.putShort(s)
            }
            fos.write(byteBuffer.array())
        }
    }

    /**
     * Copies a user-selected audio file from device storage into the app's internal private storage.
     * This avoids any subsequent URI permission revocation or path loss!
     */
    fun saveCustomSong(context: Context, sourceUri: Uri): MelodyOption? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "أغنية خاصة_${System.currentTimeMillis()}"

            // Query original filename
            val cursor: Cursor? = contentResolver.query(sourceUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
                    }
                }
            }

            val songsDir = File(context.filesDir, "custom_songs")
            if (!songsDir.exists()) songsDir.mkdirs()

            val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF]"), "_")
            val targetFile = File(songsDir, "${System.currentTimeMillis()}_$sanitizedName")

            val inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            if (inputStream != null) {
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                }
                val titleWithoutExt = fileName.substringBeforeLast(".")
                MelodyOption(
                    id = "custom_${targetFile.name}",
                    title = titleWithoutExt,
                    category = "custom",
                    uriOrPath = targetFile.absolutePath,
                    description = "أغنية محفوظة من ذاكرة هاتفك"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy user song: ${e.message}", e)
            null
        }
    }

    /**
     * Lists available system ringtones / alarms on device
     */
    fun getSystemRingtones(context: Context): List<MelodyOption> {
        val list = mutableListOf<MelodyOption>()
        try {
            val ringtoneMgr = RingtoneManager(context)
            ringtoneMgr.setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
            val cursor = ringtoneMgr.cursor
            val maxItems = 15 // Limit to top 15 for sleek UI
            var count = 0
            while (cursor != null && cursor.moveToNext() && count < maxItems) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uriStr = ringtoneMgr.getRingtoneUri(cursor.position)?.toString() ?: ""
                if (uriStr.isNotEmpty()) {
                    list.add(
                        MelodyOption(
                            id = "sys_$count",
                            title = title ?: "نغمة النظام ${count + 1}",
                            category = "system",
                            uriOrPath = uriStr,
                            description = "نغمة منبه رسمية من الهاتف"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system ringtones: ${e.message}")
        }
        return list
    }

    /**
     * Plays a preview of any melody or custom song
     */
    fun playPreview(
        context: Context,
        melody: MelodyOption,
        onComplete: (() -> Unit)? = null
    ) {
        stopPreview()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            }

            when {
                melody.category == "builtin" || melody.uriOrPath.startsWith("builtin:") -> {
                    val presetId = melody.id.ifEmpty { melody.uriOrPath.removePrefix("builtin:") }
                    val file = getMelodyFile(context, presetId)
                    player.setDataSource(file.absolutePath)
                }
                melody.category == "custom" -> {
                    val file = File(melody.uriOrPath)
                    if (file.exists()) {
                        player.setDataSource(file.absolutePath)
                    } else {
                        player.setDataSource(context, Uri.parse(melody.uriOrPath))
                    }
                }
                else -> {
                    player.setDataSource(context, Uri.parse(melody.uriOrPath))
                }
            }

            player.prepare()
            player.isLooping = false
            player.start()
            currentlyPlayingId = melody.id
            previewPlayer = player

            player.setOnCompletionListener {
                currentlyPlayingId = null
                previewPlayer = null
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error previewing sound: ${e.message}", e)
            currentlyPlayingId = null
            previewPlayer = null
            onComplete?.invoke()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview: ${e.message}")
        } finally {
            previewPlayer = null
            currentlyPlayingId = null
        }
    }
}
