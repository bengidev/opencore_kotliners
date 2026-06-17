package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** iOS CardChrome — thin border, paper fill, no shadow. */
@Composable
internal fun CardChrome(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val palette = OnboardingTheme.palette

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.surfacePaper)
            .border(1.dp, palette.lineSoft, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}
