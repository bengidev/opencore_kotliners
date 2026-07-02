package io.github.bengidev.opencore.speech.infrastructure

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.bengidev.opencore.speech.utilities.SpeechAudioLevelNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Continuous microphone level meter for the recording waveform.
 *
 * Runs independently of [android.speech.SpeechRecognizer] so the waveform stays smooth
 * while the recognizer restarts between utterance segments.
 */
internal open class SpeechAudioLevelMonitor(
    private val sampleRateHz: Int = SAMPLE_RATE_HZ,
    private val emitIntervalMs: Long = EMIT_INTERVAL_MS,
) {
    private var audioRecord: AudioRecord? = null
    private var monitorJob: Job? = null

    val isRunning: Boolean
        get() = monitorJob?.isActive == true

    open fun start(scope: CoroutineScope, onLevel: (Float) -> Unit): Boolean {
        stop()

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return false

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        audioRecord = record
        record.startRecording()
        monitorJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(minBufferSize / 2)
            var lastEmittedAtMs = 0L
            while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastEmittedAtMs >= emitIntervalMs) {
                        lastEmittedAtMs = now
                        val level = SpeechAudioLevelNormalizer.fromPcmSamples(buffer, read)
                        withContext(Dispatchers.Main.immediate) {
                            onLevel(level)
                        }
                    }
                }
            }
        }
        return true
    }

    open fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        val record = audioRecord
        audioRecord = null
        if (record == null) return
        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }
        record.release()
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val EMIT_INTERVAL_MS = 40L
    }
}
