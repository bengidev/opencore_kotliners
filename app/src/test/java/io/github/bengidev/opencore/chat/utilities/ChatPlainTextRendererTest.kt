package io.github.bengidev.opencore.chat.utilities

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.LightOpenCorePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChatPlainTextRendererTest {
    private val palette = LightOpenCorePalette

    @Test
    fun spanned_usesPrimaryColor() {
        val text = "Streaming answer text"
        val rendered = ChatPlainTextRenderer.spanned(text, palette)
        assertEquals(text, rendered.toString())

        val color = rendered.getSpans(0, text.length, ForegroundColorSpan::class.java).single()
        assertEquals(palette.textPrimary.toArgb(), color.foregroundColor)
    }

    @Test
    fun spannedThinking_usesMonospaceItalic() {
        val text = "Reasoning in progress"
        val rendered = ChatPlainTextRenderer.spannedThinking(text, palette)
        assertEquals(text, rendered.toString())

        val spans = rendered.getSpans(0, text.length, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })
        assertTrue(spans.any { it is StyleSpan && it.style == Typeface.ITALIC })

        val color = rendered.getSpans(0, text.length, ForegroundColorSpan::class.java).single()
        assertEquals(palette.textSecondary.toArgb(), color.foregroundColor)
    }
}
