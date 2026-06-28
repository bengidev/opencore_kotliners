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

private const val CODE_BLOCK_HORIZONTAL_PADDING_PX = 8

internal fun isAllowedMarkdownLink(destination: String): Boolean =
    destination.substringBefore(":", "").lowercase() in ALLOWED_MARKDOWN_LINK_SCHEMES

private val ALLOWED_MARKDOWN_LINK_SCHEMES = setOf("https", "http", "mailto")

internal class ChatAssistantMarkdownSpanVisitor(
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
        renderInlineChildren(link) { spans -> linkSpans(link.destination.orEmpty(), spans) }
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
                    val link = child
                    renderInlineChildren(link, extraSpans) { spans ->
                        linkSpans(link.destination.orEmpty(), spanTransform(spans))
                    }
                }
                else -> child.accept(this)
            }
            child = child.next
        }
    }

    private fun linkSpans(destination: String, baseSpans: List<Any>): List<Any> =
        if (isAllowedMarkdownLink(destination)) {
            baseSpans + ForegroundColorSpan(accentPrimary) + URLSpan(destination)
        } else {
            baseSpans
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
