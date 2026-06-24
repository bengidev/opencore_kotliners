package io.github.bengidev.opencore.chat.utilities

/** One streamed content block from an OpenAI-compatible `delta.content` array. */
internal data class ChatStreamContentPart(
    val type: String?,
    val text: String?
) {
    val renderedText: String?
        get() {
            val value = text?.takeIf { it.isNotEmpty() } ?: return null
            return when (type) {
                null, "text", "output_text" -> value
                else -> null
            }
        }
}
