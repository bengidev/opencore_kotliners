package io.github.bengidev.opencore.speech.domain

/** Provider-scoped configuration for post-recording Whisper transcription. */
internal data class SpeechRemoteTranscriptionContext(
    val providerId: String,
    val apiBaseUrl: String,
    val defaultHeaders: Map<String, String> = emptyMap(),
) {
    val audioTranscriptionsUrl: String
        get() = "${apiBaseUrl.trimEnd('/')}/audio/transcriptions"
}
