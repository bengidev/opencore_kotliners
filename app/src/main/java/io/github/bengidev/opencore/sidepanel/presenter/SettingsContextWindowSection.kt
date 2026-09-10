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

@Composable
internal fun SettingsContextWindowSection(
    preference: SettingsContextCompactionPreference,
    onAutoCompactionChanged: (Boolean) -> Unit,
    onThresholdPercentChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val thresholdPercent = preference.triggerThresholdPercent
    val thresholdSliderEnabled = !preference.isEnabled

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
                text = "Manage how older conversation history is summarized when the model's context window fills up.",
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
                    text = "Automatic compaction",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                )
                Text(
                    text = "Summarize older turns when context nears the model limit and reinject the summary so the session can continue.",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-compaction-threshold"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Compact when full",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (thresholdSliderEnabled) palette.textPrimary else palette.textTertiary,
                )
                Text(
                    text = "$thresholdPercent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (thresholdSliderEnabled) palette.textSecondary else palette.textTertiary,
                    modifier = Modifier.testTag("settings-compaction-threshold-value"),
                )
            }
            Text(
                text = thresholdSliderDescription(preference, thresholdSliderEnabled),
                fontSize = 12.sp,
                color = if (thresholdSliderEnabled) palette.textSecondary else palette.textTertiary,
            )
            Slider(
                value = thresholdPercent.toFloat(),
                onValueChange = { raw ->
                    onThresholdPercentChanged(raw.roundToInt())
                },
                enabled = thresholdSliderEnabled,
                valueRange = SettingsContextCompactionPreference.thresholdPercentRange.first.toFloat()..
                    SettingsContextCompactionPreference.thresholdPercentRange.last.toFloat(),
                steps = (SettingsContextCompactionPreference.thresholdPercentRange.last -
                    SettingsContextCompactionPreference.thresholdPercentRange.first) / 5 - 1,
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

        Text(
            text = compactionOptionsFooter(preference),
            fontSize = 12.sp,
            color = palette.textSecondary,
        )
    }
}

private fun thresholdSliderDescription(
    preference: SettingsContextCompactionPreference,
    sliderEnabled: Boolean,
): String {
    if (sliderEnabled) {
        return "Fill level that triggers automatic compaction when it is turned on."
    }
    return "Automatic compaction uses the ${preference.triggerThresholdPercent}% threshold. " +
        "Turn off automatic compaction to adjust it."
}

private fun compactionOptionsFooter(preference: SettingsContextCompactionPreference): String {
    if (preference.isEnabled) {
        return "Automatic compaction runs before send when context use passes this threshold. " +
            "You can also compact manually from the composer."
    }
    return "Manual compaction is available from the composer at any time. " +
        "The threshold above applies when automatic compaction is turned on."
}
