package io.github.bengidev.opencore.chat.utilities

internal object ChatStreamingMarkdownGuard {
    internal fun shouldUsePlainFallback(text: String): Boolean {
        var inFence = false
        var inlineBackticks = 0
        var index = 0
        while (index < text.length) {
            if (text.startsWith("```", index)) {
                inFence = !inFence
                inlineBackticks = 0
                index += 3
                continue
            }
            if (!inFence) {
                if (text[index] == '`') {
                    inlineBackticks++
                }
            }
            index++
        }
        if (!inFence && inlineBackticks % 2 != 0) return true
        return inFence
    }
}
