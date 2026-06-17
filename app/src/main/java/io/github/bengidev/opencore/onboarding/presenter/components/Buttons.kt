package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Primary CTA — iOS PrimaryButtonStyle: 52dp, 8dp radius, mono semibold. */
@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val palette = OnboardingTheme.palette
    val bgColor = if (enabled) palette.controlStrong else palette.controlStrong.copy(alpha = 0.4f)
    val textColor = if (enabled) palette.controlStrongText else palette.controlStrongText.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = OnboardingTheme.typography.labelMd,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(9.dp))
                trailingIcon()
            }
        }
    }
}

/** Secondary CTA — iOS SecondaryButtonStyle. */
@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val palette = OnboardingTheme.palette
    val textColor = if (enabled) palette.textPrimary else palette.textTertiary

    Box(
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceRaised.copy(alpha = 0.8f))
            .border(1.dp, palette.lineSoft, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = OnboardingTheme.typography.labelMd,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

/** Segmented chip — iOS idea studio tabs (6dp corner). */
@Composable
internal fun MonoChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val bgColor = if (selected) palette.controlStrong else palette.surfaceSubtle.copy(alpha = 0.4f)
    val textColor = if (selected) palette.controlStrongText else palette.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = OnboardingTheme.typography.monoXs,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
