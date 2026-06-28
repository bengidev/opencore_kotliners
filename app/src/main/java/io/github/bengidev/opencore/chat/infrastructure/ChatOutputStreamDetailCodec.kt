package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus

internal object ChatOutputStreamDetailCodec {
    fun encode(detail: ChatOutputStreamDetail): String = buildString {
        append('{')
        append("\"status\":")
        appendQuoted(detail.status.wireValue)
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
                status = ChatOutputStreamStatus.fromWire(
                    ChatJsonStringField.extract(json, "status"),
                    isComplete = isComplete,
                ),
                outputTail = ChatJsonStringField.extract(json, "outputTail").orEmpty(),
                cwd = ChatJsonStringField.extract(json, "cwd"),
                exitCode = ChatJsonIntField.extract(json, "exitCode"),
                durationMs = ChatJsonIntField.extract(json, "durationMs"),
            )
        }.getOrElse {
            ChatOutputStreamDetail(
                status = if (isComplete) ChatOutputStreamStatus.COMPLETED else ChatOutputStreamStatus.RUNNING
            )
        }
    }

    private fun StringBuilder.appendQuoted(value: String) {
        ChatJsonStringField.appendQuoted(this, value)
    }
}
