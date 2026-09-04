package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference
import kotlin.math.roundToInt

private const val RESERVE_TOKENS_MIN = 4_096
private const val RESERVE_TOKENS_MAX = 32_768
private const val KEEP_RECENT_TOKENS_MIN = 4_096
private const val KEEP_RECENT_TOKENS_MAX = 40_960
private const val TOKEN_STEP = 1_024

@Composable
internal fun SettingsContextWindowSection(
    preference: SettingsContextCompactionPreference,
    onAutoCompactionChanged: (Boolean) -> Unit,
    onReserveTokensChanged: (Int) -> Unit,
    onKeepRecentTokensChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val slidersEnabled = preference.isEnabled

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Context window",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Text(
                text = "Control how conversation history is compacted before it exceeds the model context limit.",
                fontSize = 13.sp,
                color = palette.textSecondary,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-compaction-auto-toggle"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Auto compact",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                )
                Text(
                    text = "Summarize older messages automatically when the context window fills up.",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
            }
            Switch(
                checked = preference.isEnabled,
                onCheckedChange = onAutoCompactionChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.controlStrongText,
                    checkedTrackColor = palette.controlStrong,
                    checkedBorderColor = palette.controlStrong,
                    uncheckedThumbColor = palette.surfaceRaised,
                    uncheckedTrackColor = palette.surfaceSubtle,
                    uncheckedBorderColor = palette.lineSoft,
                ),
            )
        }

        CompactionTokenSlider(
            label = "Reserve tokens",
            description = "Tokens kept free for the next model response.",
            value = preference.reserveTokens,
            valueRange = RESERVE_TOKENS_MIN.toFloat()..RESERVE_TOKENS_MAX.toFloat(),
            enabled = slidersEnabled,
            testTag = "settings-compaction-reserve-slider",
            onValueChange = onReserveTokensChanged,
        )

        CompactionTokenSlider(
            label = "Keep recent tokens",
            description = "Recent conversation history preserved verbatim during compaction.",
            value = preference.keepRecentTokens,
            valueRange = KEEP_RECENT_TOKENS_MIN.toFloat()..KEEP_RECENT_TOKENS_MAX.toFloat(),
            enabled = slidersEnabled,
            testTag = "settings-compaction-keep-recent-slider",
            onValueChange = onKeepRecentTokensChanged,
        )
    }
}

@Composable
private fun CompactionTokenSlider(
    label: String,
    description: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    testTag: String,
    onValueChange: (Int) -> Unit,
) {
    val palette = HomeTheme.palette
    val snappedValue = snapTokenCount(value, valueRange.start.toInt(), valueRange.endInclusive.toInt())
    val labelColor = if (enabled) palette.textPrimary else palette.textTertiary
    val valueColor = if (enabled) palette.textSecondary else palette.textTertiary
    val descriptionColor = if (enabled) palette.textSecondary else palette.textTertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
            )
            Text(
                text = formatTokenCount(snappedValue),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor,
            )
        }
        Text(
            text = description,
            fontSize = 12.sp,
            color = descriptionColor,
        )
        Slider(
            value = snappedValue.toFloat(),
            onValueChange = { raw ->
                onValueChange(
                    snapTokenCount(
                        raw.roundToInt(),
                        valueRange.start.toInt(),
                        valueRange.endInclusive.toInt(),
                    )
                )
            },
            enabled = enabled,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start) / TOKEN_STEP).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = palette.controlStrong,
                activeTrackColor = palette.controlStrong,
                inactiveTrackColor = palette.surfaceSubtle,
                disabledThumbColor = palette.textTertiary.copy(alpha = 0.5f),
                disabledActiveTrackColor = palette.lineSoft,
                disabledInactiveTrackColor = palette.surfaceSubtle,
            ),
        )
    }
}

private fun snapTokenCount(value: Int, min: Int, max: Int): Int {
    val clamped = value.coerceIn(min, max)
    val steps = ((clamped - min) / TOKEN_STEP.toFloat()).roundToInt()
    return (min + steps * TOKEN_STEP).coerceIn(min, max)
}

private fun formatTokenCount(tokens: Int): String {
    return if (tokens % 1_024 == 0) {
        "${tokens / 1_024}k"
    } else {
        tokens.toString()
    }
}
