package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.domain.OnboardingPage
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Card terminal rail — traffic dots, metric, breadcrumb labels. */
@Composable
internal fun TerminalHeader(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surfaceSubtle.copy(alpha = 0.5f))
            .border(1.dp, palette.lineSoft, RoundedCornerShape(6.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.accentPrimary)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.textTertiary.copy(alpha = 0.42f))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.textTertiary.copy(alpha = 0.24f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = page.metric,
            style = OnboardingTheme.typography.monoXs,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "AGENTS / PROMPTS / MODELS / REVIEW",
            style = OnboardingTheme.typography.monoXs.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
            color = palette.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
