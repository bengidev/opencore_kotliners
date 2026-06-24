package io.github.bengidev.opencore.chat.infrastructure

/** Networking primitive for SSE line parsing. Buffers raw bytes and splits on newlines. */
internal class ChatSSEDecoder {
    private val buffer = ArrayList<Byte>()
    private val newline = '\n'.code.toByte()

    fun append(data: ByteArray): List<SseEvent> {
        data.forEach { buffer.add(it) }

        val events = mutableListOf<SseEvent>()
        var lineStart = 0
        var index = 0
        while (index < buffer.size) {
            if (buffer[index] == newline) {
                val lineBytes = buffer.subList(lineStart, index).toByteArray()
                val rawLine = lineBytes.toString(Charsets.UTF_8)
                interpret(rawLine)?.let(events::add)
                lineStart = index + 1
            }
            index++
        }

        if (lineStart > 0) {
            buffer.subList(0, lineStart).clear()
        }
        return events
    }

    fun flush(): List<SseEvent> {
        if (buffer.isEmpty()) return emptyList()
        val rawLine = buffer.toByteArray().toString(Charsets.UTF_8)
        buffer.clear()
        return listOfNotNull(interpret(rawLine))
    }

    sealed class SseEvent {
        data class Data(val payload: String) : SseEvent()
        data object Done : SseEvent()
    }

    companion object {
        const val DONE_SENTINEL = "[DONE]"

        fun interpret(rawLine: String): SseEvent? {
            val line = if (rawLine.endsWith("\r")) rawLine.dropLast(1) else rawLine

            if (line.isEmpty()) return null
            if (line.startsWith(":")) return null
            if (!line.startsWith("data:")) return null

            var payload = line.removePrefix("data:")
            if (payload.startsWith(" ")) payload = payload.drop(1)

            if (payload == DONE_SENTINEL) return SseEvent.Done
            return SseEvent.Data(payload)
        }
    }
}
