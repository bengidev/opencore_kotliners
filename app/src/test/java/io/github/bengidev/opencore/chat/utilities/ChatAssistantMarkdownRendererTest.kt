package io.github.bengidev.opencore.chat.utilities

import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.DarkOpenCorePalette
import io.github.bengidev.opencore.onboarding.theme.LightOpenCorePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChatAssistantMarkdownRendererTest {
    private val palette = LightOpenCorePalette

    @Test
    fun inlineCode_usesMonospacedSecondaryStyling() {
        val rendered = ChatAssistantMarkdownRenderer.spanned("Use `foo` here.", palette)
        val start = rendered.indexOf("foo")
        assertNotEquals(-1, start)

        val spans = rendered.getSpans(start, start + 3, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })

        val color = rendered.getSpans(start, start + 3, ForegroundColorSpan::class.java).single()
        assertEquals(palette.textSecondary.toArgb(), color.foregroundColor)
    }

    @Test
    fun strongText_usesSemiboldWeight() {
        val rendered = ChatAssistantMarkdownRenderer.spanned("**Important** note.", palette)
        val start = rendered.indexOf("Important")
        assertNotEquals(-1, start)

        val styleSpan = rendered.getSpans(start, start + "Important".length, StyleSpan::class.java).single()
        assertEquals(Typeface.BOLD, styleSpan.style)
    }

    @Test
    fun plainProse_keepsBodyStyling() {
        val markdown = "GeForce is NVIDIA's brand for consumer GPUs."
        val rendered = ChatAssistantMarkdownRenderer.spanned(markdown, palette)
        assertEquals(markdown, rendered.toString())

        val spans = rendered.getSpans(0, 1, Any::class.java)
        assertTrue(spans.none { it is TypefaceSpan && it.family == "monospace" })
        assertTrue(spans.none { it is StyleSpan && it.style == Typeface.BOLD })

        val color = rendered.getSpans(0, 1, ForegroundColorSpan::class.java).single()
        assertEquals(palette.textPrimary.toArgb(), color.foregroundColor)
    }

    @Test
    fun unclosedInlineBacktick_fallsBackToPlainBody() {
        val markdown = "Streaming `partial token"
        val rendered = ChatAssistantMarkdownRenderer.spanned(markdown, palette)
        assertEquals(markdown, rendered.toString())

        val spans = rendered.getSpans(0, 1, Any::class.java)
        assertTrue(spans.none { it is TypefaceSpan && it.family == "monospace" })
    }

    @Test
    fun fencedCodeBlock_usesMonospacedBlockStyling() {
        val markdown = """
            Before

            ```
            let x = 1
            ```

            After
        """.trimIndent()
        val rendered = ChatAssistantMarkdownRenderer.spanned(markdown, palette)
        val start = rendered.indexOf("let x = 1")
        assertNotEquals(-1, start)

        val spans = rendered.getSpans(start, start + "let x = 1".length, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })

        val color = rendered.getSpans(start, start + "let x = 1".length, ForegroundColorSpan::class.java).single()
        assertEquals(palette.textSecondary.toArgb(), color.foregroundColor)

        val background = rendered.getSpans(start, start + "let x = 1".length, BackgroundColorSpan::class.java).single()
        assertEquals(palette.surfaceSubtle.toArgb(), background.backgroundColor)
    }

    @Test
    fun strongInlineCode_keepsMonospacedSemiboldStyling() {
        val rendered = ChatAssistantMarkdownRenderer.spanned("**`token`**", palette)
        val start = rendered.indexOf("token")
        assertNotEquals(-1, start)

        val spans = rendered.getSpans(start, start + "token".length, Any::class.java)
        assertTrue(spans.any { it is TypefaceSpan && it.family == "monospace" })
        assertTrue(spans.any { it is StyleSpan && it.style == Typeface.BOLD })
    }

    @Test
    fun disallowedLinkSchemes_areNotLinked() {
        val rendered = ChatAssistantMarkdownRenderer.spanned(
            "[bad](javascript:alert(1)) and [ok](https://example.com)",
            palette,
        )
        val badStart = rendered.indexOf("bad")
        val okStart = rendered.indexOf("ok")
        assertNotEquals(-1, badStart)
        assertNotEquals(-1, okStart)

        assertNull(rendered.getSpans(badStart, badStart + 3, URLSpan::class.java).firstOrNull())
        assertTrue(rendered.getSpans(okStart, okStart + 2, URLSpan::class.java).isNotEmpty())
    }

    @Test
    fun darkPalette_usesPrimaryTextColor() {
        val rendered = ChatAssistantMarkdownRenderer.spanned("Hello", DarkOpenCorePalette)
        val color = rendered.getSpans(0, 1, ForegroundColorSpan::class.java).single()
        assertEquals(DarkOpenCorePalette.textPrimary.toArgb(), color.foregroundColor)
    }

    @Test
    fun spanned_reusesCachedInstance_forIdenticalInput() {
        val markdown = "Cached prose stays stable."
        val first = ChatAssistantMarkdownRenderer.spanned(markdown, palette)
        val second = ChatAssistantMarkdownRenderer.spanned(markdown, palette)
        assertTrue(first === second)
    }

    @Test
    fun spanned_cacheSeparatesLightAndDarkPalettes() {
        val markdown = "Theme-specific cache key."
        val light = ChatAssistantMarkdownRenderer.spanned(markdown, LightOpenCorePalette)
        val dark = ChatAssistantMarkdownRenderer.spanned(markdown, DarkOpenCorePalette)
        assertTrue(light !== dark)
    }

    @Test
    fun shouldUsePlainFallback_detectsUnclosedFenceInSinglePass() {
        assertTrue(ChatAssistantMarkdownRenderer.shouldUsePlainFallback("```open"))
        assertFalse(ChatAssistantMarkdownRenderer.shouldUsePlainFallback("```closed```"))
    }

    @Test
    fun shouldUsePlainFallback_detectsUnclosedInlineBacktick() {
        assertTrue(ChatAssistantMarkdownRenderer.shouldUsePlainFallback("partial `token"))
        assertFalse(ChatAssistantMarkdownRenderer.shouldUsePlainFallback("closed `token`"))
    }
}
