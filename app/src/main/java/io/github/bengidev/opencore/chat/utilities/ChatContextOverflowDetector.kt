package io.github.bengidev.opencore.chat.utilities

internal object ChatContextOverflowDetector {
    private val patterns = listOf(
        "context length",
        "context window",
        "maximum context",
        "max context",
        "token limit",
        "too many tokens",
        "context_length_exceeded",
        "maximum tokens",
    )

    fun isContextOverflow(message: String): Boolean {
        val normalized = message.lowercase()
        return patterns.any { normalized.contains(it) }
    }
}
