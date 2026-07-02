package io.github.bengidev.opencore.speech.infrastructure

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.utilities.SpeechRecognizerLocaleResolver
import io.github.bengidev.opencore.speech.utilities.SpeechAudioLevelNormalizer
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionFallbackLogic
import io.github.bengidev.opencore.speech.utilities.SpeechTranscriptAccumulator
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/** Android `SpeechRecognizer` adapter with on-device preference and audio levels. */
internal class SpeechSystemRecognitionEngine(
    private val context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val permissionRequester: suspend () -> Boolean,
) {
    private val mutex = Mutex()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var latestTranscript: String = ""
    private var committedTranscript: String = ""
    private var currentSegmentText: String = ""
    private var preferOnDevice: Boolean = true
    private var isListening: Boolean = false
    private var isIntentionalStop: Boolean = false
    private var pendingFinalResults: CompletableDeferred<Unit>? = null
    private val pcmBufferRecorder = SpeechPcmBufferRecorder(context)

    fun authorizationStatus(): SpeechAuthorizationStatus {
        return when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> SpeechAuthorizationStatus.AUTHORIZED
            else -> SpeechAuthorizationStatus.NOT_DETERMINED
        }
    }

    suspend fun requestAuthorization(): SpeechAuthorizationStatus {
        if (authorizationStatus() == SpeechAuthorizationStatus.AUTHORIZED) {
            return SpeechAuthorizationStatus.AUTHORIZED
        }
        return if (permissionRequester()) {
            SpeechAuthorizationStatus.AUTHORIZED
        } else {
            SpeechAuthorizationStatus.DENIED
        }
    }

    fun start(): Flow<SpeechRecognitionEvent> = callbackFlow {
        mutex.withLock {
            tearDownRecognizer()
            latestTranscript = ""
            committedTranscript = ""
            currentSegmentText = ""
            preferOnDevice = shouldPreferOnDeviceRecognition()

            if (!canAttemptRecognition()) {
                trySend(
                    SpeechRecognitionEvent.Failed(
                        SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                            systemMessage = SpeechSystemRecognitionError.RecognizerUnavailable.message,
                        ),
                    ),
                )
                close()
                return@callbackFlow
            }

            val resolvedLocale = resolvedLocale(locale)
            if (resolvedLocale == null) {
                trySend(
                    SpeechRecognitionEvent.Failed(
                        SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                            systemMessage = SpeechSystemRecognitionError.LocaleUnavailable.message,
                        ),
                    ),
                )
                close()
                return@callbackFlow
            }

            val recognizer = createRecognizer()
            if (recognizer == null) {
                trySend(
                    SpeechRecognitionEvent.Failed(
                        SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                            systemMessage = SpeechSystemRecognitionError.RecognizerUnavailable.message,
                        ),
                    ),
                )
                close()
                return@callbackFlow
            }

            speechRecognizer = recognizer
            pcmBufferRecorder.start()
            val listener = createListener(
                resolvedLocale = resolvedLocale,
                onEvent = { event -> trySend(event) },
                onComplete = { close() },
            )
            recognizer.setRecognitionListener(listener)
            beginListening(recognizer, resolvedLocale, preferOnDevice)
        }

        awaitClose {
            tearDownRecognizer()
        }
    }

    suspend fun stop(): SpeechRecognitionResult? = mutex.withLock {
        isListening = false
        isIntentionalStop = true

        try {
            val recognizer = speechRecognizer
            if (recognizer != null) {
                val finalResults = CompletableDeferred<Unit>()
                pendingFinalResults = finalResults
                runOnMainThread { recognizer.stopListening() }
                withTimeoutOrNull(FINAL_RESULTS_TIMEOUT_MS) { finalResults.await() }
                pendingFinalResults = null
            }

            val transcript = latestTranscript.trim()
            val (pcmFile, pcmDuration) = pcmBufferRecorder.finish()
            releaseRecognizer()

            latestTranscript = ""
            committedTranscript = ""
            currentSegmentText = ""
            if (transcript.isBlank() && pcmFile == null) return@withLock null
            SpeechRecognitionResult(
                transcript = transcript,
                audioFilePath = pcmFile?.absolutePath,
                durationSeconds = pcmDuration,
            )
        } finally {
            isIntentionalStop = false
        }
    }

    private fun canAttemptRecognition(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) || hasRecognizerIntentHandler()
    }

    private fun shouldPreferOnDeviceRecognition(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun hasRecognizerIntentHandler(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    private fun createRecognizer(): SpeechRecognizer? {
        return runOnMainThread {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun resolvedLocale(preferred: Locale): Locale? {
        return SpeechRecognizerLocaleResolver.resolve(preferred = preferred) { true }
    }

    private fun beginListening(
        recognizer: SpeechRecognizer,
        resolvedLocale: Locale,
        preferOffline: Boolean,
    ) {
        preferOnDevice = preferOffline
        isListening = true
        runOnMainThread {
            recognizer.startListening(recognitionIntent(resolvedLocale, preferOffline))
        }
    }

    private fun recognitionIntent(locale: Locale, preferOnDevice: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Keep each session alive longer so continuous dictation restarts less often.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5_000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2_000)
            if (preferOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    private fun createListener(
        resolvedLocale: Locale,
        onEvent: (SpeechRecognitionEvent) -> Unit,
        onComplete: () -> Unit,
    ): RecognitionListener = object : RecognitionListener {
        private var hasSignaledReady = false
        private var lastAudioLevelEmittedAtMs = 0L
        private var lastBufferLevelAtMs = 0L
        private var lastRestartAtMs = 0L

        override fun onReadyForSpeech(params: Bundle?) {
            if (!hasSignaledReady) {
                hasSignaledReady = true
                onEvent(SpeechRecognitionEvent.Ready)
            }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            val now = System.currentTimeMillis()
            if (now - lastBufferLevelAtMs < AUDIO_LEVEL_BUFFER_PRIORITY_MS) return
            emitAudioLevel(SpeechAudioLevelNormalizer.fromRmsDecibels(rmsdB), now)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            if (buffer == null || buffer.isEmpty()) return
            pcmBufferRecorder.append(buffer)
            val now = System.currentTimeMillis()
            lastBufferLevelAtMs = now
            emitAudioLevel(SpeechAudioLevelNormalizer.fromPcmBuffer(buffer), now)
        }

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            signalFinalResults()

            val isBenignEnd =
                error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (isIntentionalStop && isBenignEnd) {
                return
            }

            if (isListening && isBenignEnd) {
                restartIfStillListening(resolvedLocale)
                return
            }

            val message = errorMessage(error)
            if (
                SpeechRecognitionFallbackLogic.shouldRetryWithServerRecognition(
                    errorMessage = message,
                    attemptedOnDevice = preferOnDevice,
                    errorCode = error,
                )
            ) {
                retryWithServerRecognition(resolvedLocale, onEvent, onComplete)
                return
            }

            onEvent(
                SpeechRecognitionEvent.Failed(
                    SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                        systemMessage = message,
                        attemptedOnDevice = preferOnDevice,
                    ),
                ),
            )
            tearDownRecognizer()
            onComplete()
        }

        override fun onResults(results: Bundle?) {
            val text = results?.bestText().orEmpty()
            if (text.isNotBlank()) {
                committedTranscript = SpeechTranscriptAccumulator.commitSegment(committedTranscript, text)
                currentSegmentText = ""
                latestTranscript = committedTranscript
                onEvent(SpeechRecognitionEvent.Final(latestTranscript))
            }
            signalFinalResults()

            if (!isListening) return
            if (text.isBlank() && latestTranscript.isNotBlank()) return
            restartIfStillListening(resolvedLocale)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.bestText().orEmpty()
            if (text.isBlank()) return
            currentSegmentText = text
            latestTranscript = SpeechTranscriptAccumulator.displayTranscript(
                committed = committedTranscript,
                currentSegment = currentSegmentText,
            )
            onEvent(SpeechRecognitionEvent.Partial(latestTranscript))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        private fun emitAudioLevel(level: Float, nowMs: Long = System.currentTimeMillis()) {
            if (nowMs - lastAudioLevelEmittedAtMs < AUDIO_LEVEL_EMIT_INTERVAL_MS) return
            lastAudioLevelEmittedAtMs = nowMs
            onEvent(SpeechRecognitionEvent.AudioLevel(level))
        }

        private fun restartIfStillListening(resolvedLocale: Locale) {
            val recognizer = speechRecognizer
            if (!isListening || recognizer == null) {
                tearDownRecognizer()
                onComplete()
                return
            }

            val now = System.currentTimeMillis()
            val elapsedSinceRestart = now - lastRestartAtMs
            if (elapsedSinceRestart < MIN_RESTART_INTERVAL_MS) {
                mainHandler.postDelayed(
                    { restartIfStillListening(resolvedLocale) },
                    MIN_RESTART_INTERVAL_MS - elapsedSinceRestart,
                )
                return
            }
            lastRestartAtMs = now

            runOnMainThread {
                if (!isListening || speechRecognizer !== recognizer) return@runOnMainThread
                recognizer.startListening(recognitionIntent(resolvedLocale, preferOnDevice))
            }
        }
    }

    private fun retryWithServerRecognition(
        resolvedLocale: Locale,
        onEvent: (SpeechRecognitionEvent) -> Unit,
        onComplete: () -> Unit,
    ) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            onEvent(
                SpeechRecognitionEvent.Failed(
                    SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                        systemMessage = SpeechSystemRecognitionError.RecognizerUnavailable.message,
                    ),
                ),
            )
            onComplete()
            return
        }

        preferOnDevice = false
        try {
            beginListening(recognizer, resolvedLocale, preferOffline = false)
        } catch (error: Exception) {
            onEvent(
                SpeechRecognitionEvent.Failed(
                    SpeechRecognitionFallbackLogic.userFacingErrorMessage(
                        systemMessage = error.message.orEmpty(),
                        attemptedOnDevice = false,
                    ),
                ),
            )
            tearDownRecognizer()
            onComplete()
        }
    }

    private fun signalFinalResults() {
        pendingFinalResults?.complete(Unit)
    }

    private fun tearDownRecognizer() {
        isListening = false
        signalFinalResults()
        pcmBufferRecorder.discard()
        releaseRecognizer()
    }

    private fun releaseRecognizer() {
        val recognizer = speechRecognizer
        speechRecognizer = null
        if (recognizer != null) {
            runOnMainThread {
                recognizer.setRecognitionListener(null)
                recognizer.destroy()
            }
        }
    }

    private fun <T> runOnMainThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        val result = java.util.concurrent.ArrayBlockingQueue<ResultHolder<T>>(1)
        mainHandler.post {
            try {
                result.put(ResultHolder(block(), null))
            } catch (error: Exception) {
                result.put(ResultHolder(null, error))
            }
        }
        val holder = result.take()
        holder.error?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return holder.value as T
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording failed."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language is not supported for speech recognition."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many speech recognition requests."
            else -> "Speech recognition failed."
        }
    }

    private fun Bundle.bestText(): String? {
        val matches = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        return matches.firstOrNull()
    }

    private data class ResultHolder<T>(val value: T?, val error: Exception?)
}

private enum class SpeechSystemRecognitionError(val message: String) {
    LocaleUnavailable("Speech recognition is not available for your language on this device."),
    RecognizerUnavailable("Speech recognition is temporarily unavailable."),
}

private const val AUDIO_LEVEL_EMIT_INTERVAL_MS = 50L
private const val AUDIO_LEVEL_BUFFER_PRIORITY_MS = 33L
private const val FINAL_RESULTS_TIMEOUT_MS = 1_500L
private const val MIN_RESTART_INTERVAL_MS = 400L
