package io.github.bengidev.opencore.chat.utilities

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

/**
 * Native markdown styling for assistant answer prose (inline code, emphasis, blocks).
 * Mirrors iOS `ChatAssistantMarkdownRenderer`.
 */
internal object ChatAssistantMarkdownRenderer {
    private const val CODE_BLOCK_HORIZONTAL_PADDING_PX = 8
    private val allowedLinkSchemes = setOf("https", "http", "mailto")
    private val cache = BoundedCache()
    private val parser = Parser.builder().build()

    fun spanned(markdown: String, palette: OpenCorePalette): Spanned {
        val canCache = !shouldUsePlainFallback(markdown)
        if (canCache) {
            cache.valueFor(markdown, palette.isDark)?.let { return it }
        }

        val rendered = render(markdown, palette)
        if (canCache) {
            cache.store(rendered, markdown, palette.isDark)
        }
        return rendered
    }

    private fun render(markdown: String, palette: OpenCorePalette): Spanned {
        if (shouldUsePlainFallback(markdown)) {
            return plainBody(markdown, palette)
        }

        return try {
            val visitor = MarkdownSpanVisitor(palette)
            parser.parse(markdown).accept(visitor)
            visitor.build()
        } catch (_: Exception) {
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
        val fenceDelimiter = "```"
        val fenceCount = text.split(fenceDelimiter).size - 1
        if (fenceCount % 2 != 0) return true

        text.split(fenceDelimiter).forEachIndexed { index, segment ->
            if (index % 2 == 0 && segment.count { it == '`' } % 2 != 0) {
                return true
            }
        }
        return false
    }

    private class MarkdownSpanVisitor(
        private val palette: OpenCorePalette,
        private val builder: SpannableStringBuilder = SpannableStringBuilder(),
    ) : AbstractVisitor() {
        private val textPrimary = palette.textPrimary.toArgb()
        private val textSecondary = palette.textSecondary.toArgb()
        private val accentPrimary = palette.accentPrimary.toArgb()
        private val surfaceSubtle = palette.surfaceSubtle.toArgb()

        fun build(): Spanned = builder

        override fun visit(text: Text) {
            appendWithSpans(text.literal, bodySpans())
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append('\n')
        }

        override fun visit(code: Code) {
            appendWithSpans(code.literal, inlineCodeSpans(bold = hasBoldAncestor(code)))
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            renderInlineChildren(strongEmphasis, extraSpans = listOf(StyleSpan(Typeface.BOLD)))
        }

        override fun visit(emphasis: Emphasis) {
            renderInlineChildren(emphasis, extraSpans = listOf(StyleSpan(Typeface.ITALIC)))
        }

        override fun visit(link: Link) {
            val destination = link.destination.orEmpty()
            val allowed = destination.substringBefore(":", "").lowercase() in allowedLinkSchemes
            renderInlineChildren(link) { spans ->
                if (allowed) spans + ForegroundColorSpan(accentPrimary) + URLSpan(destination) else spans
            }
        }

        override fun visit(heading: Heading) {
            renderInlineChildren(heading, extraSpans = listOf(StyleSpan(Typeface.BOLD)))
            builder.append('\n')
        }

        override fun visit(fencedCodeBlock: FencedCodeBlock) {
            val literal = fencedCodeBlock.literal.orEmpty().trimEnd('\n')
            if (literal.isEmpty()) return
            val start = builder.length
            builder.append(literal)
            val end = builder.length
            builder.setSpan(TypefaceSpan("monospace"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(textSecondary), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(BackgroundColorSpan(surfaceSubtle), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(
                LeadingMarginSpan.Standard(CODE_BLOCK_HORIZONTAL_PADDING_PX, CODE_BLOCK_HORIZONTAL_PADDING_PX),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            builder.append('\n')
        }

        override fun visit(paragraph: Paragraph) {
            super.visit(paragraph)
            if (paragraph.next != null) {
                builder.append('\n')
            }
        }

        private fun renderInlineChildren(
            node: Node,
            extraSpans: List<Any> = emptyList(),
            spanTransform: (List<Any>) -> List<Any> = { it },
        ) {
            var child = node.firstChild
            while (child != null) {
                when (child) {
                    is Text -> appendWithSpans(child.literal, spanTransform(bodySpans() + extraSpans))
                    is Code -> appendWithSpans(
                        child.literal,
                        spanTransform(inlineCodeSpans(bold = extraSpans.any { it is StyleSpan && it.style == Typeface.BOLD })),
                    )
                    is StrongEmphasis -> renderInlineChildren(child, extraSpans + StyleSpan(Typeface.BOLD), spanTransform)
                    is Emphasis -> renderInlineChildren(child, extraSpans + StyleSpan(Typeface.ITALIC), spanTransform)
                    is Link -> {
                        val destination = child.destination.orEmpty()
                        val allowed = destination.substringBefore(":", "").lowercase() in allowedLinkSchemes
                        renderInlineChildren(child, extraSpans) { spans ->
                            val merged = spanTransform(spans)
                            if (allowed) merged + ForegroundColorSpan(accentPrimary) + URLSpan(destination) else merged
                        }
                    }
                    else -> {
                        val nestedVisitor = MarkdownSpanVisitor(palette)
                        child.accept(nestedVisitor)
                        appendWithSpans(nestedVisitor.build().toString(), spanTransform(bodySpans() + extraSpans))
                    }
                }
                child = child.next
            }
        }

        private fun bodySpans(): List<Any> = listOf(ForegroundColorSpan(textPrimary))

        private fun inlineCodeSpans(bold: Boolean): List<Any> = buildList {
            add(TypefaceSpan("monospace"))
            add(ForegroundColorSpan(textSecondary))
            if (bold) add(StyleSpan(Typeface.BOLD))
        }

        private fun appendWithSpans(text: String, spans: List<Any>) {
            if (text.isEmpty()) return
            val start = builder.length
            builder.append(text)
            spans.forEach { span ->
                builder.setSpan(span, start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        private fun hasBoldAncestor(node: Node): Boolean {
            var parent = node.parent
            while (parent != null) {
                if (parent is StrongEmphasis) return true
                parent = parent.parent
            }
            return false
        }
    }
}

private class BoundedCache {
    private data class Key(val content: String, val isDark: Boolean)

    private val limit = 64
    private val storage = LinkedHashMap<Key, Spanned>()
    private val order = ArrayDeque<Key>()

    fun valueFor(content: String, isDark: Boolean): Spanned? = storage[Key(content, isDark)]

    fun store(value: Spanned, content: String, isDark: Boolean) {
        val key = Key(content, isDark)
        if (storage.containsKey(key)) {
            order.remove(key)
        }
        storage[key] = value
        order.addLast(key)
        while (order.size > limit) {
            storage.remove(order.removeFirst())
        }
    }
}
