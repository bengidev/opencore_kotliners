package io.github.bengidev.opencore.chat.utilities

import org.json.JSONArray

/** Normalizes assistant wire text before it reaches the chat bubble. */
internal object ChatAssistantContentNormalizer {
    const val SAFETY_ONLY_FALLBACK =
        "This model returned a safety check instead of an answer. Try another model or rephrase your question."

    private val safetyLinePattern =
        Regex("""^(?i)(user|response) safety:\s*\S+$""")
    private val textFieldPattern =
        Regex("""(?:'text'|"text")\s*:\s*(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)')""")

    fun displayText(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return raw

        extractFromContentBlocks(trimmed)?.takeIf { it.isNotEmpty() }?.let { return it }
        if (isSafetyOnlyOutput(trimmed)) return SAFETY_ONLY_FALLBACK
        return raw
    }

    fun isSafetyOnlyOutput(text: String): Boolean {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return false
        return lines.all { safetyLinePattern.matches(it) }
    }

    fun extractFromContentBlocks(text: String): String? {
        val trimmed = text.trim()
        if (!trimmed.startsWith('[') || !trimmed.endsWith(']')) return null
        if (!trimmed.contains("text", ignoreCase = true)) return null

        runCatching {
            val blocks = JSONArray(trimmed)
            val joined = (0 until blocks.length())
                .mapNotNull { index ->
                    val objectJson = blocks.optJSONObject(index) ?: return@mapNotNull null
                    ChatStreamContentPart(
                        type = objectJson.optString("type").takeIf { it.isNotEmpty() },
                        text = objectJson.optString("text").takeIf { it.isNotEmpty() }
                    ).renderedText
                }
                .joinToString("")
            if (joined.isNotEmpty()) return joined
        }

        val regexExtracted = extractTextFields(trimmed)
        return regexExtracted.takeIf { it.isNotEmpty() }
    }

    private fun extractTextFields(blockArray: String): String =
        textFieldPattern.findAll(blockArray)
            .mapNotNull { match ->
                match.groups[1]?.value ?: match.groups[2]?.value
            }
            .joinToString("")
}
