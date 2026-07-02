package io.github.bengidev.opencore.speech.utilities

/** Joins committed and in-flight recognizer segments for continuous dictation. */
internal object SpeechTranscriptAccumulator {
    fun displayTranscript(committed: String, currentSegment: String): String {
        val segment = currentSegment.trim()
        if (segment.isEmpty()) return committed.trim()
        if (committed.isEmpty()) return segment
        if (segment.startsWith(committed)) return segment
        if (committed.endsWith(segment)) return committed
        return "$committed $segment"
    }

    fun commitSegment(committed: String, finalizedSegment: String): String {
        return displayTranscript(committed, finalizedSegment)
    }
}
