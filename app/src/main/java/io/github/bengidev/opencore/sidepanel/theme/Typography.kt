package io.github.bengidev.opencore.sidepanel.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
internal data class SidePanelTypography(
    val sessionTitle: TextStyle,
    val sessionPreview: TextStyle,
    val settingsTitle: TextStyle,
    val settingsLabel: TextStyle,
    val settingsBody: TextStyle,
    val chipLabel: TextStyle,
    val monoLabel: TextStyle
)

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

internal val DefaultSidePanelTypography = SidePanelTypography(
    sessionTitle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    sessionPreview = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    settingsTitle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    settingsLabel = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp
    ),
    settingsBody = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    chipLabel = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp
    ),
    monoLabel = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

internal val LocalSidePanelTypography = staticCompositionLocalOf { DefaultSidePanelTypography }
