package io.github.bengidev.opencore.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Shrinks type to fit like iOS `minimumScaleFactor` before ellipsizing. */
@Composable
internal fun ScaleToFitText(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = style.fontSize,
            stepSize = 1.sp
        )
    )
}
