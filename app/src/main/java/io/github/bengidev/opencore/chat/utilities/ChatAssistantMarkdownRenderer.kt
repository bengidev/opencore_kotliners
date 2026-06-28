package io.github.bengidev.opencore.chat.utilities

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import org.commonmark.parser.Parser

/**
 * Native markdown styling for assistant answer prose (inline code, emphasis, blocks).
 * Mirrors iOS `ChatAssistantMarkdownRenderer`.
 */
internal object ChatAssistantMarkdownRenderer {
    private val cache = BoundedSpannedCache()
    private val parser = Parser.builder().build()

    fun spanned(markdown: String, palette: OpenCorePalette): Spanned {
        val canCache = !shouldUsePlainFallback(markdown)
        if (canCache) {
            cache.get(markdown, palette.isDark)?.let { return it }
        }

        val rendered = render(markdown, palette)
        if (canCache) {
            cache.put(markdown, palette.isDark, rendered)
        }
        return rendered
    }

    private fun render(markdown: String, palette: OpenCorePalette): Spanned {
        if (shouldUsePlainFallback(markdown)) {
            return plainBody(markdown, palette)
        }

        return try {
            val visitor = ChatAssistantMarkdownSpanVisitor(palette)
            parser.parse(markdown).accept(visitor)
            visitor.build()
        } catch (_: IllegalStateException) {
            plainBody(markdown, palette)
        }
    }

    private fun plainBody(text: String, palette: OpenCorePalette): Spanned =
        SpannableStringBuilder(text).apply {
            setSpan(
                ForegroundColorSpan(palette.textPrimary.toArgb()),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

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
                when (text[index]) {
                    '`' -> inlineBackticks++
                    else -> {
                        if (inlineBackticks % 2 != 0) return true
                        inlineBackticks = 0
                    }
                }
            }
            index++
        }
        if (!inFence && inlineBackticks % 2 != 0) return true
        return inFence
    }
}
