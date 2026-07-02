package io.github.bengidev.opencore.speech.utilities

/** Merges a voice transcript into an existing composer draft. */
internal object SpeechComposerDraftMerger {
    fun merged(existing: String, transcript: String): String {
        val trimmedTranscript = transcript.trim()
        if (trimmedTranscript.isEmpty()) return existing
        if (existing.isEmpty()) return trimmedTranscript
        if (existing.endsWith(' ')) return existing + trimmedTranscript
        return "$existing $trimmedTranscript"
    }
}
