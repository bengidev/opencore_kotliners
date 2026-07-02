package io.github.bengidev.opencore.speech.application

import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechCaptureResult
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingLimits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Owns composer speech-to-text lifecycle — permissions, listening state, and transcript delivery. */
internal class SpeechFlowController(
    private val recognitionFactory: suspend () -> SpeechRecognitionClient,
    private val scope: CoroutineScope,
    private val autoStopThresholdSeconds: Double = SpeechRecordingLimits.autoStopThresholdSeconds,
) {
    private val _state = MutableStateFlow(SpeechFlowState())
    val state: StateFlow<SpeechFlowState> = _state.asStateFlow()

    private val sessionMutex = Mutex()
    private var recognitionJob: Job? = null
    private var durationJob: Job? = null
    private var startJob: Job? = null
    private var stopJob: Job? = null
    private var recognitionSessionId: ULong = 0uL
    private var captureGeneration: ULong = 0uL
    private var autoStopTriggered = false
    private var sessionRecognition: SpeechRecognitionClient? = null
    private var recordingStartedAtNanos: Long = 0L
    private var voiceCaptureHandler: ((SpeechCaptureResult) -> Unit)? = null

    fun setVoiceCaptureHandler(handler: ((SpeechCaptureResult) -> Unit)?) {
        voiceCaptureHandler = handler
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun startListening() {
        startJob?.let { job ->
            if (job.isActive) return
        }

        if (recognitionJob != null || stopJob?.isActive == true) return

        startJob = scope.launch {
            performStartListening()
        }
    }

    private suspend fun performStartListening() {
        clearError()

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

        if (recognitionJob != null || stopJob?.isActive == true) return

        captureGeneration += 1uL

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
        recordingStartedAtNanos = System.nanoTime()

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
                            if (_state.value.isTranscribing || stopJob?.isActive == true) return@collect
                            recognitionJob?.cancel()
                            recognitionJob = null
                            _state.update { it.copy(errorMessage = event.message) }
                            resetListeningPresentation()
                            return@collect
                        }
                        is SpeechRecognitionEvent.AudioLevel -> applyAudioLevel(event.level)
                    }
                }
            } catch (_: CancellationException) {
                // Session replaced or cancelled.
            } catch (error: Throwable) {
                if (recognitionSessionId == sessionId && !_state.value.isTranscribing) {
                    _state.update {
                        it.copy(
                            errorMessage = error.message?.takeIf { message -> message.isNotBlank() }
                                ?: "Voice recording could not be started.",
                        )
                    }
                    resetListeningPresentation()
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

        if (stopJob?.isActive == true) return null
        val current = _state.value
        if (!current.isListening && !current.isTranscribing && recognitionJob == null) {
            return null
        }

        val generation = captureGeneration
        var capture: SpeechCaptureResult? = null
        stopJob = scope.launch {
            capture = runStopListening(generation)
        }
        stopJob?.join()
        stopJob = null
        return capture
    }

    private suspend fun runStopListening(generation: ULong): SpeechCaptureResult? {
        try {
            val waveformSamples = _state.value.audioLevels
            val duration = currentElapsedDurationSeconds()
            val capturedPartial = _state.value.partialTranscript.trim()
            _state.update {
                it.copy(
                    transcribingWaveformSamples = waveformSamples,
                    transcribingDurationSeconds = duration,
                    isTranscribing = true,
                    isListening = false,
                )
            }

            val result = finishListening()
            if (generation != captureGeneration) {
                discardRecordedAudio(result)
                return null
            }

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

            if (generation != captureGeneration) return null

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

            if (generation != captureGeneration) return null

            val completed = SpeechCaptureResult(composerText = transcript)
            _state.update { it.copy(errorMessage = null) }
            voiceCaptureHandler?.invoke(completed)
            return completed
        } catch (_: CancellationException) {
            resetListeningPresentation()
            return null
        } finally {
            sessionRecognition = null
        }
    }

    suspend fun cancelListening() {
        sessionMutex.withLock {
            captureGeneration += 1uL
            stopJob?.cancel()
            stopJob?.join()
            stopJob = null
            startJob?.cancel()
            startJob?.join()
            startJob = null
            val result = finishListening()
            discardRecordedAudio(result)
            resetListeningPresentation()
            sessionRecognition = null
        }
    }

    private suspend fun finishListening(): SpeechRecognitionResult? {
        stopDurationTimer()
        val recognition = sessionRecognition
        val result = recognition?.stop()
        recognitionJob?.cancel()
        recognitionJob?.join()
        recognitionJob = null
        resetListeningPresentation(clearTranscribing = false)
        recordingStartedAtNanos = 0L
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
        recordingStartedAtNanos = 0L
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(100)
                if (!_state.value.isListening) return@launch
                _state.update {
                    it.copy(elapsedDurationSeconds = currentElapsedDurationSeconds())
                }
                handleAutoStopIfNeeded()
            }
        }
    }

    private fun currentElapsedDurationSeconds(): Double {
        val startedAt = recordingStartedAtNanos
        if (startedAt <= 0L) return _state.value.elapsedDurationSeconds
        return (System.nanoTime() - startedAt) / 1_000_000_000.0
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
    }
}
