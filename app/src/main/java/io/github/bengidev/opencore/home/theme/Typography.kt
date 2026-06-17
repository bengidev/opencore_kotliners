package io.github.bengidev.opencore.home.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
internal data class HomeTypography(
    val welcomeTitle: TextStyle,
    val welcomeCaption: TextStyle,
    val composerBody: TextStyle,
    val chipLabel: TextStyle,
    val contextUsage: TextStyle
)

private val Mono = FontFamily.Monospace
private val Sans = FontFamily.SansSerif

internal val DefaultHomeTypography = HomeTypography(
    welcomeTitle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp
    ),
    welcomeCaption = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    composerBody = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    chipLabel = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp
    ),
    contextUsage = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 8.sp,
        lineHeight = 10.sp
    )
)

internal val LocalHomeTypography = staticCompositionLocalOf { DefaultHomeTypography }
