package io.github.bengidev.opencore.chat.utilities

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

/**
 * Plain text styling for streaming surfaces before markdown is safe to render.
 */
internal object ChatPlainTextRenderer {
    fun spanned(text: String, palette: OpenCorePalette): Spanned =
        SpannableStringBuilder(text).apply {
            setSpan(
                ForegroundColorSpan(palette.textPrimary.toArgb()),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

    fun spannedThinking(text: String, palette: OpenCorePalette): Spanned =
        SpannableStringBuilder(text).apply {
            setSpan(
                TypefaceSpan("monospace"),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.ITALIC),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                ForegroundColorSpan(palette.textSecondary.toArgb()),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
}
