package io.github.bengidev.opencore.home.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun Modifier.homeComposerGlass(
    cornerRadius: Dp,
    shadowOpacity: Float = 0.16f
): Modifier {
    val palette = HomeTheme.palette
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val fillAlpha = if (palette.isDark) 0.70f else 0.72f
    val strokeAlpha = if (palette.isDark) 0.35f else 0.55f

    return this
        .shadow(
            elevation = 18.dp,
            shape = shape,
            ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = shadowOpacity),
            spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = shadowOpacity)
        )
        .clip(shape)
        .background(palette.surfaceRaised.copy(alpha = fillAlpha))
        .border(1.dp, palette.lineSoft.copy(alpha = strokeAlpha), shape)
}
