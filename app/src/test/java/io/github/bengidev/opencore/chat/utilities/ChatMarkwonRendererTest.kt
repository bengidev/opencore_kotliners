package io.github.bengidev.opencore.chat.utilities

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.widget.TextView
import io.github.bengidev.opencore.onboarding.theme.LightOpenCorePalette
import io.noties.markwon.ext.tables.TableRowSpan
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChatMarkwonRendererTest {
    private val palette = LightOpenCorePalette
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun assistant_inlineCode_isMonospaced() {
        val rendered = ChatMarkwonRenderer.spanned(
            markdown = "Use `foo` here.",
            palette = palette,
            profile = ChatMarkwonRenderer.Profile.Assistant,
            context = context,
        )
        val start = rendered.indexOf("foo")
        assertNotEquals(-1, start)

        val spans = rendered.getSpans(start, start + 3, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })
    }

    @Test
    fun thinking_body_isMonospaceItalic() {
        val markdown = "Reasoning step one."
        val rendered = ChatMarkwonRenderer.spanned(
            markdown = markdown,
            palette = palette,
            profile = ChatMarkwonRenderer.Profile.Thinking,
            context = context,
        )
        val end = rendered.length
        assertTrue(end > 0)

        val spans = rendered.getSpans(0, end, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })
        assertTrue(spans.any { it is StyleSpan && it.style == Typeface.ITALIC })
    }

    @Test
    fun assistant_httpsLinks_areClickable() {
        val rendered = ChatMarkwonRenderer.spanned(
            markdown = "See https://example.com for details.",
            palette = palette,
            profile = ChatMarkwonRenderer.Profile.Assistant,
            context = context,
        )
        val start = rendered.indexOf("https://example.com")
        assertNotEquals(-1, start)

        val urlSpans = rendered.getSpans(start, start + "https://example.com".length, URLSpan::class.java)
        assertTrue(urlSpans.isNotEmpty())
    }

    @Test
    fun assistant_applyTo_schedulesTableRowSpans() {
        val markdown =
            """
            | Hyper-parameter | Typical range / notes |
            |---|---|
            | vocab_size | 30k-200k |
            | num_heads | embedding_dim ÷ 64 |
            """.trimIndent()
        val textView = TextView(context)

        ChatMarkwonRenderer.applyTo(
            textView = textView,
            markdown = markdown,
            palette = palette,
            profile = ChatMarkwonRenderer.Profile.Assistant,
            context = context,
        )

        val tableSpans = (textView.text as android.text.Spanned).getSpans(0, textView.text.length, TableRowSpan::class.java)
        assertTrue(tableSpans.isNotEmpty())
    }
}
