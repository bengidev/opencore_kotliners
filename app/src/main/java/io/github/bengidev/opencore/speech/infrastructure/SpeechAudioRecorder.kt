package io.github.bengidev.opencore.speech.infrastructure

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/** Records microphone audio to a temporary file for voice-note attachments. */
internal class SpeechAudioRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtNanos: Long = 0L

    val isActive: Boolean
        get() = recorder != null

    fun start(): File? {
        stop()
        val file = File(context.cacheDir, "voice-note-${UUID.randomUUID()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return runCatching {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            outputFile = file
            startedAtNanos = System.nanoTime()
            file
        }.getOrElse {
            mediaRecorder.release()
            null
        }
    }

    /** Max PCM amplitude since the previous poll; only valid while [isActive]. */
    fun pollMaxAmplitude(): Int {
        return runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
    }

    fun stop(): Pair<File?, Double> {
        val file = outputFile
        val duration = if (startedAtNanos > 0L) {
            (System.nanoTime() - startedAtNanos) / 1_000_000_000.0
        } else {
            0.0
        }
        runCatching {
            recorder?.stop()
        }
        recorder?.release()
        recorder = null
        outputFile = null
        startedAtNanos = 0L
        return file to duration
    }
}
