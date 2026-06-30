package io.github.bengidev.opencore.speech.application

import android.content.Context
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentStore
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic
import io.github.bengidev.opencore.speech.utilities.SpeechSilentWavGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Owns composer speech-to-text lifecycle — permissions, listening state, and voice-note attachments. */
internal class SpeechFlowController(
    private val recognition: SpeechRecognitionClient,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    private val _state = MutableStateFlow(SpeechFlowState())
    val state: StateFlow<SpeechFlowState> = _state.asStateFlow()

    private var recognitionJob: Job? = null
    private var durationJob: Job? = null

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun displayedDraft(base: String): String {
        val current = _state.value
        if (!current.isListening || current.partialTranscript.isEmpty()) return base
        return mergedDraft(existing = base, transcript = current.partialTranscript)
    }

    fun startListening() {
        if (recognitionJob != null) return
        clearError()

        var status = recognition.authorizationStatus()
        recognitionJob = scope.launch {
            if (status == SpeechAuthorizationStatus.NOT_DETERMINED) {
                status = recognition.requestAuthorization()
            }

            if (status != SpeechAuthorizationStatus.AUTHORIZED) {
                _state.update { it.copy(errorMessage = PERMISSION_DENIED_MESSAGE) }
                recognitionJob = null
                return@launch
            }

            _state.update {
                it.copy(
                    partialTranscript = "",
                    elapsedDurationSeconds = 0.0,
                    audioLevels = emptyList(),
                    isVoiceActive = false,
                )
            }

            try {
                recognition.start().collect { event ->
                    when (event) {
                        SpeechRecognitionEvent.Ready -> {
                            _state.update { it.copy(isListening = true) }
                            startDurationTimer()
                        }
                        is SpeechRecognitionEvent.Partial -> {
                            _state.update { it.copy(partialTranscript = event.text) }
                        }
                        is SpeechRecognitionEvent.Final -> {
                            _state.update { it.copy(partialTranscript = event.text) }
                        }
                        is SpeechRecognitionEvent.Failed -> {
                            _state.update { it.copy(errorMessage = event.message) }
                            recognition.stop()
                            resetListeningPresentation()
                            return@collect
                        }
                        is SpeechRecognitionEvent.AudioLevel -> applyAudioLevel(event.level)
                    }
                }
                recognition.stop()
                resetListeningPresentation()
            } finally {
                recognitionJob = null
                stopDurationTimer()
            }
        }
    }

    suspend fun stopListening(): ChatMessageAttachment? {
        val waveformSamples = _state.value.audioLevels
        val duration = _state.value.elapsedDurationSeconds
        val result = finishListening()
        val attachment = makeVoiceAttachment(
            result = result,
            waveformSamples = waveformSamples,
            duration = duration,
        )
        if (attachment == null &&
            result?.audioFilePath != null &&
            result.transcript.isBlank()
        ) {
            _state.update {
                it.copy(errorMessage = "Voice note could not be transcribed. Try again or type your message.")
            }
        }
        return attachment
    }

    suspend fun cancelListening() {
        finishListening()
    }

    private suspend fun finishListening(): SpeechRecognitionResult? {
        stopDurationTimer()
        val result = recognition.stop()
        recognitionJob?.cancel()
        recognitionJob = null
        resetListeningPresentation()
        return result
    }

    private fun applyAudioLevel(level: Float) {
        _state.update { current ->
            current.copy(
                isVoiceActive = SpeechRecordingDisplayLogic.isVoiceActive(level),
                audioLevels = SpeechRecordingDisplayLogic.appendWaveformSample(
                    level = level,
                    levels = current.audioLevels,
                    capacity = SpeechRecordingDisplayLogic.WAVEFORM_SAMPLE_CAPACITY,
                ),
            )
        }
    }

    private fun resetListeningPresentation() {
        _state.update {
            it.copy(
                isListening = false,
                partialTranscript = "",
                elapsedDurationSeconds = 0.0,
                audioLevels = emptyList(),
                isVoiceActive = false,
            )
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        val startedAt = System.nanoTime()
        durationJob = scope.launch {
            while (true) {
                delay(100)
                if (!_state.value.isListening) return@launch
                val elapsed = (System.nanoTime() - startedAt) / 1_000_000_000.0
                _state.update { it.copy(elapsedDurationSeconds = elapsed) }
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    private fun makeVoiceAttachment(
        result: SpeechRecognitionResult?,
        waveformSamples: List<Float>,
        duration: Double,
    ): ChatMessageAttachment? {
        val transcript = result?.transcript?.trim().orEmpty()
        if (transcript.isEmpty()) return null
        val audioFile = resolveVoiceNoteAudioFile(result, duration) ?: return null

        val stored = runCatching {
            ChatAttachmentStore.save(
                context,
                copyingFrom = audioFile,
                suggestedFilename = audioFile.name,
            )
        }.getOrNull() ?: return null
        runCatching { audioFile.delete() }

        return ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.AUDIO,
            filename = "Voice note",
            localPath = stored.absolutePath,
            waveformSamples = waveformSamples,
            audioDurationSeconds = maxOf(duration, result?.durationSeconds ?: 0.0),
            speechTranscript = transcript,
        )
    }

    private fun resolveVoiceNoteAudioFile(
        result: SpeechRecognitionResult?,
        duration: Double,
    ): File? {
        result?.audioFilePath?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > 0L) return file
        }
        val effectiveDuration = maxOf(duration, result?.durationSeconds ?: 0.0)
        return SpeechSilentWavGenerator.create(context, durationSeconds = effectiveDuration)
    }

    companion object {
        private const val PERMISSION_DENIED_MESSAGE =
            "Microphone access is required for voice input."

        fun mergedDraft(existing: String, transcript: String): String {
            val trimmedTranscript = transcript.trim()
            if (trimmedTranscript.isEmpty()) return existing
            if (existing.isEmpty()) return trimmedTranscript
            if (existing.endsWith(' ')) return existing + trimmedTranscript
            return "$existing $trimmedTranscript"
        }
    }
}
