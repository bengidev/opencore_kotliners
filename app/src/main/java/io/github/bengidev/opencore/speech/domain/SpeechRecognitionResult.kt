package io.github.bengidev.opencore.speech.domain

/** Speech stop payload — transcript for model input and optional captured audio. */
internal data class SpeechRecognitionResult(
    val transcript: String,
    val audioFilePath: String? = null,
    val durationSeconds: Double = 0.0,
    /** Set when post-recording transcription fails (e.g. remote API error). */
    val failureMessage: String? = null,
)
