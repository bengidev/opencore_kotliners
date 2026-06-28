package io.github.bengidev.opencore.chat.domain

internal enum class ChatOutputStreamStatus {
    RUNNING,
    COMPLETED,
    FAILED;

    val wireValue: String
        get() = name.lowercase()

    companion object {
        fun fromWire(value: String?, isComplete: Boolean = true): ChatOutputStreamStatus =
            entries.firstOrNull { it.wireValue == value?.lowercase() }
                ?: if (isComplete) COMPLETED else RUNNING

        fun fromProvider(raw: String?, exitCode: Int?): ChatOutputStreamStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                "failed", "error", "failure" -> FAILED
                "completed", "complete", "success", "succeeded", "ok" -> COMPLETED
                "running", "in_progress", "in-progress" -> RUNNING
                else -> if (exitCode != null && exitCode != 0) {
                    FAILED
                } else {
                    COMPLETED
                }
            }
        }
    }
}
