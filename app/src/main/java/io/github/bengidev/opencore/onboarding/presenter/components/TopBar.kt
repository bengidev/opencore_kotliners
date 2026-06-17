package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Top bar — iOS OnboardingTopBarView. */
@Composable
internal fun OnboardingTopBar(
    currentPage: Int,
    totalPages: Int,
    onSkip: () -> Unit,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            ThemeToggleButton(onClick = onThemeToggle)

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "OPENCORE",
                    style = OnboardingTheme.typography.monoSm.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = "AI ASSISTANCE",
                    style = OnboardingTheme.typography.monoXs,
                    color = palette.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "PG.${String.format("%02d", currentPage + 1)} / ${String.format("%02d", totalPages)}",
            style = OnboardingTheme.typography.monoSm,
            color = palette.textSecondary,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "SKIP",
            style = OnboardingTheme.typography.monoSm.copy(fontSize = 11.sp),
            color = palette.textSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(palette.surfaceSubtle.copy(alpha = 0.4f))
                .border(1.dp, palette.lineSoft, RoundedCornerShape(6.dp))
                .clickable(onClick = onSkip)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
