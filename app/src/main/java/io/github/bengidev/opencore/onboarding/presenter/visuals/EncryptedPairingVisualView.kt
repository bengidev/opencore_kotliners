package io.github.bengidev.opencore.onboarding.presenter.visuals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

private val PairingSpring = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessMedium
)

/** Encrypted pairing demo — device nodes, dashed channel, traveling dot with glow. */
@Composable
internal fun EncryptedPairingVisualView(
    pairingConfirmed: Boolean,
    onTogglePairing: () -> Unit,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    appeared: Boolean = true
) {
    val palette = OnboardingTheme.palette
    val radius = OnboardingTheme.radius
    val accent = if (pairingConfirmed) palette.accentPrimary else palette.warning

    val leftEntryOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else -24f,
        animationSpec = PairingSpring,
        label = "pairing_left_entry"
    )
    val rightEntryOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = PairingSpring,
        label = "pairing_right_entry"
    )
    val centerAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = 80),
        label = "pairing_center_alpha"
    )
    val dotTravelOffset by animateFloatAsState(
        targetValue = if (pairingConfirmed) 56f else -56f,
        animationSpec = PairingSpring,
        label = "pairing_dot_travel"
    )
    val shieldScale by animateFloatAsState(
        targetValue = if (pairingConfirmed) 1f else 0.94f,
        animationSpec = PairingSpring,
        label = "pairing_shield_scale"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pairing_pulse")
    val dotPulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pairingConfirmed) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pairing_dot_pulse"
    )
    val dotPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (pairingConfirmed) 0.85f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pairing_dot_glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "End-to-end encrypted local-to-OpenCore pairing diagram" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceNode(
                icon = Icons.Filled.PhoneAndroid,
                title = "LOCAL",
                subtitle = "Local key",
                active = pairingConfirmed,
                modifier = Modifier.graphicsLayer { translationX = leftEntryOffset.dp.toPx() }
            )
            Spacer(modifier = Modifier.weight(1f))
            DeviceNode(
                icon = Icons.Filled.Laptop,
                title = "OPENCORE",
                subtitle = "AI chat lane",
                active = pairingConfirmed,
                modifier = Modifier.graphicsLayer { translationX = rightEntryOffset.dp.toPx() }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = centerAlpha
                    scaleX = 0.92f + (centerAlpha * 0.08f)
                    scaleY = 0.92f + (centerAlpha * 0.08f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashedConnectionSegment(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = palette.lineSoft.copy(alpha = 0.8f),
                        animated = pairingConfirmed
                    )
                    Spacer(modifier = Modifier.width(82.dp))
                    DashedConnectionSegment(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = palette.lineSoft.copy(alpha = 0.8f),
                        animated = pairingConfirmed
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = dotTravelOffset.dp)
                        .scale(dotPulseScale)
                        .shadow(
                            elevation = if (pairingConfirmed) 10.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = palette.accentPrimary.copy(alpha = dotPulseAlpha),
                            spotColor = palette.accentPrimary.copy(alpha = dotPulseAlpha)
                        )
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(palette.accentPrimary)
                )

                Box(
                    modifier = Modifier
                        .scale(shieldScale)
                        .size(82.dp)
                        .shadow(
                            elevation = if (pairingConfirmed) 6.dp else 2.dp,
                            shape = RoundedCornerShape(radius.sm),
                            ambientColor = accent.copy(alpha = 0.18f),
                            spotColor = accent.copy(alpha = 0.24f)
                        )
                        .clip(RoundedCornerShape(radius.sm))
                        .background(palette.surfaceRaised)
                        .border(1.dp, palette.lineSoft, RoundedCornerShape(radius.sm))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePairing
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pairingConfirmed) Icons.Filled.Shield else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            PairingActionButton(
                pairingConfirmed = pairingConfirmed,
                accent = accent,
                onClick = onActionButtonClick
            )
        }
    }
}

@Composable
private fun DashedConnectionSegment(
    color: Color,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseTransition = rememberInfiniteTransition(label = "dash_phase")
    val dashPhase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 24f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase_offset"
    )

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        drawLine(
            color = color,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(5.dp.toPx(), 7.dp.toPx()),
                phase = dashPhase
            )
        )
    }
}

@Composable
private fun DeviceNode(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val radius = OnboardingTheme.radius

    val borderColor by animateColorAsState(
        targetValue = if (active) palette.accentPrimary.copy(alpha = 0.52f) else palette.lineSoft,
        animationSpec = tween(320),
        label = "device_border"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) palette.textPrimary else palette.textTertiary,
        animationSpec = tween(320),
        label = "device_icon"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.97f,
        animationSpec = PairingSpring,
        label = "device_scale"
    )

    Column(
        modifier = modifier
            .width(108.dp)
            .scale(cardScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 92.dp)
                .shadow(
                    elevation = if (active) 4.dp else 0.dp,
                    shape = RoundedCornerShape(radius.sm),
                    ambientColor = palette.accentPrimary.copy(alpha = 0.12f),
                    spotColor = palette.accentPrimary.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(radius.sm))
                .background(palette.surfaceSubtle.copy(alpha = if (active) 0.62f else 0.5f))
                .border(1.dp, borderColor, RoundedCornerShape(radius.sm)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = OnboardingTheme.typography.monoXs,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = OnboardingTheme.typography.monoXs.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp
                ),
                color = palette.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PairingActionButton(
    pairingConfirmed: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = PairingSpring,
        label = "cta_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = if (pairingConfirmed) Icons.Filled.Refresh else Icons.Filled.Link,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (pairingConfirmed) "ROTATE KEY" else "PAIR DEVICE",
            style = OnboardingTheme.typography.monoSm,
            color = accent
        )
    }
}
