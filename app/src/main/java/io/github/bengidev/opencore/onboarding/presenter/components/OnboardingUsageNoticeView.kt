package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

internal object OnboardingUsageNoticeCopy {
    const val notice =
        "The default key is free to start with, but daily token and turn limits apply. " +
            "Bring your own API key (BYOK) in Settings for higher limits and full provider access."

    const val voiceOver =
        "The default key is free to start with, but daily token and turn limits apply. " +
            "Bring your own API key in Settings for higher limits and full provider access."
}

@Composable
internal fun OnboardingUsageNoticeView(
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    Text(
        text = OnboardingUsageNoticeCopy.notice,
        color = palette.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        modifier = modifier.semantics {
            contentDescription = OnboardingUsageNoticeCopy.voiceOver
        }
    )
}
