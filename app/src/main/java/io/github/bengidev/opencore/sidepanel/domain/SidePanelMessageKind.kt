package io.github.bengidev.opencore.sidepanel.domain

/** Persisted message kind discriminator for conversation history. */
internal enum class SidePanelMessageKind {
    TEXT,
    THINKING,
    SYSTEM;

    val wireValue: String
        get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): SidePanelMessageKind =
            entries.firstOrNull { it.wireValue == value?.lowercase() } ?: TEXT
    }
}
