package io.github.bengidev.opencore.speech.domain

/** Speech stop payload — transcript for model input, audio for bubble display. */
internal data class SpeechRecognitionResult(
    val transcript: String,
    val audioFilePath: String? = null,
    val durationSeconds: Double = 0.0,
)
