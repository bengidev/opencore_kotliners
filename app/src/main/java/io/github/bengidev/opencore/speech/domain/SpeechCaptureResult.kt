package io.github.bengidev.opencore.speech.domain

/** Outcome of a completed voice capture — transcript text for the composer draft. */
internal data class SpeechCaptureResult(
    val composerText: String,
)
