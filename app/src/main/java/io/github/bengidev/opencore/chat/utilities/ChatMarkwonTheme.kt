package io.github.bengidev.opencore.chat.utilities

import androidx.compose.ui.graphics.toArgb
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import io.noties.markwon.core.MarkwonTheme

internal object ChatMarkwonTheme {
    fun apply(
        builder: MarkwonTheme.Builder,
        palette: OpenCorePalette,
        profile: ChatMarkwonRenderer.Profile,
    ) {
        applyPalette(builder, palette, profile)
    }

    private fun applyPalette(
        builder: MarkwonTheme.Builder,
        palette: OpenCorePalette,
        profile: ChatMarkwonRenderer.Profile,
    ) {
        val textSecondary = palette.textSecondary.toArgb()
        val accentPrimary = palette.accentPrimary.toArgb()
        val surfaceSubtle = palette.surfaceSubtle.toArgb()

        builder
            .linkColor(accentPrimary)
            .codeTextColor(textSecondary)
            .codeBlockTextColor(textSecondary)
            .codeBackgroundColor(surfaceSubtle)
            .codeBlockBackgroundColor(surfaceSubtle)
    }
}
