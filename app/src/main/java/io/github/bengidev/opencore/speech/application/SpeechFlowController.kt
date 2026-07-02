package io.github.bengidev.opencore.speech.application

import android.content.Context
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechCaptureResult
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingLimits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Owns composer speech-to-text lifecycle — permissions, listening state, and transcript delivery. */
internal class SpeechFlowController(
    private val recognitionFactory: suspend () -> SpeechRecognitionClient,
    private val scope: CoroutineScope,
    private val context: Context,
    private val autoStopThresholdSeconds: Double = SpeechRecordingLimits.autoStopThresholdSeconds,
    private val microphoneAuthorizationStatus: () -> SpeechAuthorizationStatus = {
        SpeechAuthorizationStatus.AUTHORIZED
    },
    private val requestMicrophoneAuthorization: suspend () -> SpeechAuthorizationStatus = {
        SpeechAuthorizationStatus.AUTHORIZED
    },
) {
    private val _state = MutableStateFlow(SpeechFlowState())
    val state: StateFlow<SpeechFlowState> = _state.asStateFlow()

    private var recognitionJob: Job? = null
    private var durationJob: Job? = null
    private var startJob: Job? = null
    private var recognitionSessionId: ULong = 0uL
    private var autoStopTriggered = false
    private var isStopping = false
    private var sessionRecognition: SpeechRecognitionClient? = null

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearPendingCapture() {
        _state.update { it.copy(pendingCapture = null) }
    }

    /** Composer draft stays user-typed only; live transcript is not mirrored into the text field. */
    fun displayedDraft(base: String): String = base

    fun startListening() {
        startJob?.let { job ->
            if (job.isActive) return
        }

        if (recognitionJob != null) return

        val job = scope.launch {
            performStartListening()
        }
        startJob = job
    }

    private suspend fun performStartListening() {
        clearError()
        clearPendingCapture()

        val recognition = recognitionFactory()
        sessionRecognition = recognition

        var status = recognition.authorizationStatus()
        if (status == SpeechAuthorizationStatus.NOT_DETERMINED) {
            status = recognition.requestAuthorization()
        }

        if (status != SpeechAuthorizationStatus.AUTHORIZED) {
            resetListeningPresentation()
            _state.update { it.copy(errorMessage = PERMISSION_DENIED_MESSAGE) }
            return
        }

        var microphoneStatus = microphoneAuthorizationStatus()
        if (microphoneStatus == SpeechAuthorizationStatus.NOT_DETERMINED) {
            microphoneStatus = requestMicrophoneAuthorization()
        }

        if (microphoneStatus != SpeechAuthorizationStatus.AUTHORIZED) {
            resetListeningPresentation()
            _state.update { it.copy(errorMessage = PERMISSION_DENIED_MESSAGE) }
            return
        }

        if (recognitionJob != null) return

        _state.update {
            it.copy(
                partialTranscript = "",
                elapsedDurationSeconds = 0.0,
                audioLevels = emptyList(),
                isVoiceActive = false,
                isTranscribing = false,
                isListening = true,
            )
        }
        autoStopTriggered = false

        recognitionSessionId += 1uL
        val sessionId = recognitionSessionId

        recognitionJob = scope.launch {
            try {
                recognition.start().collect { event ->
                    when (event) {
                        SpeechRecognitionEvent.Ready -> {
                            startDurationTimer()
                        }
                        is SpeechRecognitionEvent.Partial -> {
                            _state.update { it.copy(partialTranscript = event.text) }
                        }
                        is SpeechRecognitionEvent.Final -> {
                            _state.update { it.copy(partialTranscript = event.text) }
                        }
                        is SpeechRecognitionEvent.Failed -> {
                            if (isStopping || _state.value.isTranscribing) return@collect
                            recognitionJob?.cancel()
                            recognitionJob = null
                            _state.update { it.copy(errorMessage = event.message) }
                            resetListeningPresentation()
                            return@collect
                        }
                        is SpeechRecognitionEvent.AudioLevel -> applyAudioLevel(event.level)
                    }
                }
            } finally {
                if (recognitionSessionId == sessionId) {
                    recognitionJob = null
                    stopDurationTimer()
                }
            }
        }
    }

    suspend fun stopListening(): SpeechCaptureResult? {
        startJob?.join()

        if (isStopping) return null
        val current = _state.value
        if (!current.isListening && !current.isTranscribing && recognitionJob == null) {
            return null
        }

        isStopping = true
        try {
            val waveformSamples = current.audioLevels
            val duration = current.elapsedDurationSeconds
            val capturedPartial = current.partialTranscript.trim()
            _state.update {
                it.copy(
                    transcribingWaveformSamples = waveformSamples,
                    transcribingDurationSeconds = duration,
                    isTranscribing = true,
                    isListening = false,
                )
            }

            val result = finishListening()
            _state.update {
                it.copy(
                    isTranscribing = false,
                    transcribingWaveformSamples = emptyList(),
                    transcribingDurationSeconds = 0.0,
                )
            }

            val enrichedResult = enrichResult(
                result = result,
                partialTranscript = capturedPartial,
                durationSeconds = duration,
            )
            discardRecordedAudio(enrichedResult)

            enrichedResult?.failureMessage?.let { failureMessage ->
                val transcript = enrichedResult.transcript.trim()
                if (transcript.isEmpty()) {
                    _state.update { it.copy(errorMessage = failureMessage) }
                    return null
                }
            }

            val transcript = enrichedResult?.transcript?.trim().orEmpty()
            if (transcript.isEmpty()) {
                _state.update {
                    it.copy(errorMessage = "No speech was detected. Try again or type your message.")
                }
                return null
            }

            val capture = SpeechCaptureResult(composerText = transcript)
            _state.update { it.copy(errorMessage = null, pendingCapture = capture) }
            return capture
        } finally {
            isStopping = false
            sessionRecognition = null
        }
    }

    suspend fun cancelListening() {
        startJob?.join()
        val result = finishListening()
        discardRecordedAudio(result)
        resetListeningPresentation()
        clearPendingCapture()
        sessionRecognition = null
    }

    private suspend fun finishListening(): SpeechRecognitionResult? {
        stopDurationTimer()
        val recognition = sessionRecognition
        val result = recognition?.stop()
        recognitionJob?.cancel()
        recognitionJob?.join()
        recognitionJob = null
        resetListeningPresentation(clearTranscribing = false)
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

    private fun resetListeningPresentation(clearTranscribing: Boolean = true) {
        _state.update { current ->
            current.copy(
                isListening = false,
                isTranscribing = if (clearTranscribing) false else current.isTranscribing,
                transcribingWaveformSamples = if (clearTranscribing) emptyList() else current.transcribingWaveformSamples,
                transcribingDurationSeconds = if (clearTranscribing) 0.0 else current.transcribingDurationSeconds,
                partialTranscript = "",
                elapsedDurationSeconds = 0.0,
                audioLevels = emptyList(),
                isVoiceActive = false,
            )
        }
        autoStopTriggered = false
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        var elapsedSeconds = 0.0
        durationJob = scope.launch {
            while (true) {
                delay(100)
                if (!_state.value.isListening) return@launch
                elapsedSeconds += 0.1
                _state.update { it.copy(elapsedDurationSeconds = elapsedSeconds) }
                handleAutoStopIfNeeded()
            }
        }
    }

    private fun handleAutoStopIfNeeded() {
        val current = _state.value
        if (
            !current.isListening ||
            autoStopTriggered ||
            !SpeechRecordingLimits.shouldAutoStop(
                elapsedSeconds = current.elapsedDurationSeconds,
                thresholdSeconds = autoStopThresholdSeconds,
            )
        ) {
            return
        }

        autoStopTriggered = true
        scope.launch {
            stopListening()
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    companion object {
        private const val PERMISSION_DENIED_MESSAGE =
            "Microphone and speech recognition access are required for voice input."

        fun enrichResult(
            result: SpeechRecognitionResult?,
            partialTranscript: String,
            durationSeconds: Double,
        ): SpeechRecognitionResult? {
            result?.failureMessage?.let { failureMessage ->
                val stoppedTranscript = result.transcript.trim()
                if (stoppedTranscript.isNotEmpty()) return result
                if (partialTranscript.isEmpty()) return result
                return result.copy(transcript = partialTranscript)
            }

            val stoppedTranscript = result?.transcript?.trim().orEmpty()
            if (stoppedTranscript.isNotEmpty()) return result
            if (partialTranscript.isEmpty()) return result
            return SpeechRecognitionResult(
                transcript = partialTranscript,
                audioFilePath = result?.audioFilePath,
                durationSeconds = result?.durationSeconds ?: durationSeconds,
                failureMessage = result?.failureMessage,
            )
        }

        fun discardRecordedAudio(result: SpeechRecognitionResult?) {
            val path = result?.audioFilePath ?: return
            runCatching { File(path).delete() }
        }

        fun mergedDraft(existing: String, transcript: String): String {
            val trimmedTranscript = transcript.trim()
            if (trimmedTranscript.isEmpty()) return existing
            if (existing.isEmpty()) return trimmedTranscript
            if (existing.endsWith(' ')) return existing + trimmedTranscript
            return "$existing $trimmedTranscript"
        }
    }
}
