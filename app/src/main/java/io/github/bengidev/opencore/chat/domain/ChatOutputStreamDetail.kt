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

        fun encode(detail: ChatOutputStreamDetail): String = buildString {
            append('{')
            append("\"status\":")
            appendQuoted(statusWire(detail.status))
            append(",\"outputTail\":")
            appendQuoted(detail.outputTail)
            detail.cwd?.let {
                append(",\"cwd\":")
                appendQuoted(it)
            }
            detail.exitCode?.let {
                append(",\"exitCode\":")
                append(it)
            }
            detail.durationMs?.let {
                append(",\"durationMs\":")
                append(it)
            }
            append('}')
        }

        fun decode(json: String?, isComplete: Boolean): ChatOutputStreamDetail {
            if (json.isNullOrBlank()) {
                return ChatOutputStreamDetail(
                    status = if (isComplete) ChatOutputStreamStatus.COMPLETED else ChatOutputStreamStatus.RUNNING
                )
            }
            return runCatching {
                ChatOutputStreamDetail(
                    status = ChatOutputStreamStatus.fromWire(extractString(json, "status")),
                    outputTail = extractString(json, "outputTail").orEmpty(),
                    cwd = extractString(json, "cwd"),
                    exitCode = extractInt(json, "exitCode"),
                    durationMs = extractInt(json, "durationMs"),
                )
            }.getOrElse {
                ChatOutputStreamDetail(
                    status = if (isComplete) ChatOutputStreamStatus.COMPLETED else ChatOutputStreamStatus.RUNNING
                )
            }
        }

        private fun statusWire(status: ChatOutputStreamStatus): String = status.wireValue

        private fun extractString(json: String, key: String): String? =
            io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField.extract(json, key)

        private fun extractInt(json: String, key: String): Int? =
            io.github.bengidev.opencore.chat.infrastructure.ChatJsonIntField.extract(json, key)

        private fun StringBuilder.appendQuoted(value: String) {
            io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField.appendQuoted(this, value)
        }
    }
}
