package io.github.bengidev.opencore.chat.domain

internal data class ChatOutputStreamDetail(
    val status: ChatOutputStreamStatus = ChatOutputStreamStatus.RUNNING,
    val outputTail: String = "",
    val cwd: String? = null,
    val exitCode: Int? = null,
    val durationMs: Int? = null,
) {
    fun appendOutput(chunk: String): ChatOutputStreamDetail {
        val combined = outputTail + chunk
        return copy(outputTail = trimOutputTail(combined))
    }

    companion object {
        const val MAX_OUTPUT_LINES = 30

        fun trimOutputTail(raw: String): String {
            val lines = raw.lines()
            if (lines.size <= MAX_OUTPUT_LINES) return raw
            return lines.takeLast(MAX_OUTPUT_LINES).joinToString("\n")
        }
    }
}
