package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** iOS CardChrome — thin border, paper fill, no shadow. */
@Composable
internal fun CardChrome(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = OnboardingTheme.palette
    val shape = RoundedCornerShape(cornerRadius)

    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = palette.surfacePaper),
        border = BorderStroke(1.dp, palette.lineSoft),
        content = content,
    )
}
