package io.github.bengidev.opencore.speech.infrastructure

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechAudioLevelNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Continuous microphone capture — mirrors iOS `AVAudioEngine` input tap.
 *
 * Owns the mic for the full session (no `SpeechRecognizer` restarts) and records
 * PCM to a WAV file for post-stop Whisper transcription.
 */
internal class SpeechContinuousCaptureEngine(
    private val context: Context,
    private val sampleRateHz: Int = SAMPLE_RATE_HZ,
    private val emitIntervalMs: Long = EMIT_INTERVAL_MS,
) {
    private val mutex = Mutex()
    private val pcmBufferRecorder = SpeechPcmBufferRecorder(context, sampleRateHz)
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val isCapturing = AtomicBoolean(false)
    private var startedAtNanos: Long = 0L

    fun authorizationStatus(): SpeechAuthorizationStatus {
        return when (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        ) {
            PackageManager.PERMISSION_GRANTED -> SpeechAuthorizationStatus.AUTHORIZED
            else -> SpeechAuthorizationStatus.NOT_DETERMINED
        }
    }

    suspend fun requestAuthorization(
        permissionRequester: suspend () -> Boolean,
    ): SpeechAuthorizationStatus {
        if (authorizationStatus() == SpeechAuthorizationStatus.AUTHORIZED) {
            return SpeechAuthorizationStatus.AUTHORIZED
        }
        return if (permissionRequester()) {
            SpeechAuthorizationStatus.AUTHORIZED
        } else {
            SpeechAuthorizationStatus.DENIED
        }
    }

    fun start(scope: CoroutineScope): Flow<SpeechRecognitionEvent> = callbackFlow {
        mutex.withLock {
            tearDownCapture()
            pcmBufferRecorder.start()
            startedAtNanos = System.nanoTime()

            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBufferSize <= 0) {
                close(IllegalStateException("Audio capture is unavailable on this device."))
                return@callbackFlow
            }

            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                close(IllegalStateException("Microphone could not be initialized."))
                return@callbackFlow
            }

            audioRecord = record
            isCapturing.set(true)
            record.startRecording()
            trySend(SpeechRecognitionEvent.Ready)

            val buffer = ShortArray(minBufferSize / 2)
            var lastEmittedAtMs = 0L
            captureJob = scope.launch(Dispatchers.IO) {
                while (isActive && isCapturing.get()) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    pcmBufferRecorder.append(shortsToPcmBytes(buffer, read))
                    val now = System.currentTimeMillis()
                    if (now - lastEmittedAtMs >= emitIntervalMs) {
                        lastEmittedAtMs = now
                        val level = SpeechAudioLevelNormalizer.fromPcmSamples(buffer, read)
                        trySend(SpeechRecognitionEvent.AudioLevel(level))
                    }
                }
            }
        }

        awaitClose {
            tearDownCapture()
        }
    }

    suspend fun stop(): SpeechRecognitionResult? = mutex.withLock {
        isCapturing.set(false)
        captureJob?.cancel()
        captureJob = null

        val record = audioRecord
        audioRecord = null
        if (record != null) {
            runCatching {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
            record.release()
        }

        val (pcmFile, pcmDuration) = pcmBufferRecorder.finish()
        val duration = when {
            pcmDuration > 0.0 -> pcmDuration
            startedAtNanos > 0L -> (System.nanoTime() - startedAtNanos) / 1_000_000_000.0
            else -> 0.0
        }
        startedAtNanos = 0L

        if (pcmFile == null && duration <= 0.0) return@withLock null
        SpeechRecognitionResult(
            transcript = "",
            audioFilePath = pcmFile?.absolutePath,
            durationSeconds = duration,
        )
    }

    private fun tearDownCapture() {
        isCapturing.set(false)
        captureJob?.cancel()
        captureJob = null
        val record = audioRecord
        audioRecord = null
        if (record != null) {
            runCatching {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
            record.release()
        }
        pcmBufferRecorder.discard()
        startedAtNanos = 0L
    }

    private fun shortsToPcmBytes(samples: ShortArray, sampleCount: Int): ByteArray {
        val bytes = ByteArray(sampleCount * 2)
        for (index in 0 until sampleCount) {
            val sample = samples[index].toInt()
            bytes[index * 2] = (sample and 0xFF).toByte()
            bytes[index * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val EMIT_INTERVAL_MS = 40L
    }
}
