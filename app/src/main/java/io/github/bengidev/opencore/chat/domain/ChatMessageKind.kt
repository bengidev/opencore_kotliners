package io.github.bengidev.opencore.chat.domain

/** Persisted message kind discriminator. Mirrors iOS `ChatMessageKind`. */
internal enum class ChatMessageKind {
    TEXT,
    THINKING,
    SYSTEM;

    val wireValue: String
        get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): ChatMessageKind =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: TEXT
    }
}
