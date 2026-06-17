package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.domain.OnboardingPageType
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Eyebrow badge — iOS Badge.swift: subtle capsule, mono label, accent when active. */
@Composable
internal fun Badge(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val palette = OnboardingTheme.palette

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surfaceSubtle.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isActive) palette.accentPrimary.copy(alpha = 0.4f) else palette.lineSoft,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isActive) palette.accentPrimary else palette.textTertiary)
        )
        if (icon != null) {
            Spacer(modifier = Modifier.width(7.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) palette.accentPrimary else palette.textSecondary,
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text.uppercase(),
            style = OnboardingTheme.typography.monoXs,
            color = if (isActive) palette.accentPrimary else palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Page code chip (SEC-01, AI-02…). */
@Composable
internal fun IndexLabelChip(
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette

    Text(
        text = label,
        style = OnboardingTheme.typography.monoXs,
        color = palette.accentPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.accentSoft)
            .border(1.dp, palette.accentPrimary.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

internal fun iconForPageType(type: OnboardingPageType): ImageVector {
    return when (type) {
        OnboardingPageType.EncryptedPairing -> Icons.Filled.Shield
        OnboardingPageType.IdeaStudio -> Icons.Filled.AutoAwesome
        OnboardingPageType.PromptQueue -> Icons.Filled.PlayArrow
        OnboardingPageType.ReasoningControl -> Icons.Filled.Settings
        OnboardingPageType.WorkspaceReady -> Icons.Filled.Lock
    }
}
