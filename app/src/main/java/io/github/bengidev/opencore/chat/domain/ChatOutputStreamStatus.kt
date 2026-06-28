package io.github.bengidev.opencore.chat.domain

internal enum class ChatOutputStreamStatus {
    RUNNING,
    COMPLETED,
    FAILED;

    val wireValue: String
        get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): ChatOutputStreamStatus =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: COMPLETED
    }
}
