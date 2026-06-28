package io.github.bengidev.opencore.chat.utilities

/** One streamed content block from an OpenAI-compatible `delta.content` array. */
internal data class ChatStreamContentPart(
    val type: String?,
    val text: String?,
    val command: String? = null,
    val cwd: String? = null,
    val chunk: String? = null,
    val delta: String? = null,
    val output: String? = null,
    val status: String? = null,
    val exitCode: Int? = null,
    val durationMs: Int? = null,
) {
    val renderedText: String?
        get() {
            val value = text?.takeIf { it.isNotEmpty() } ?: return null
            return when (type) {
                null, "text", "output_text" -> value
                else -> null
            }
        }

    val resolvedCommand: String?
        get() = command?.trim()?.takeIf { it.isNotEmpty() }

    val resolvedOutputDelta: String?
        get() = sequenceOf(chunk, delta, output, text)
            .firstOrNull { !it.isNullOrEmpty() }

    val resolvedStatus: io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
        get() {
            val raw = status?.trim()?.lowercase()
            return when (raw) {
                "failed", "error", "failure" -> io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus.FAILED
                "completed", "complete", "success", "succeeded", "ok" ->
                    io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus.COMPLETED
                else -> if (exitCode != null && exitCode != 0) {
                    io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus.FAILED
                } else {
                    io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus.COMPLETED
                }
            }
        }
}
