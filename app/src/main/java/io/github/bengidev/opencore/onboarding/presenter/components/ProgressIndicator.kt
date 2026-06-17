package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** iOS-matched progress: blue pill for active page, muted dots for others. */
@Composable
internal fun OnboardingProgressIndicator(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until totalPages) {
            val isCurrent = index == currentPage

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isCurrent) 28.dp else 6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isCurrent) palette.accentPrimary else palette.lineSoft)
                    .clickable { onPageSelected(index) }
            )
        }
    }
}
