package io.github.bengidev.opencore.sidepanel.domain

/** User preferences for automatic context window compaction (Pi-aligned defaults). */
internal data class SettingsContextCompactionPreference(
    val isEnabled: Boolean = true,
    val triggerThresholdPercent: Int = 90,
    val minRecentMessages: Int = 4,
    val reserveTokens: Int = 16_384,
    val keepRecentTokens: Int = 20_000,
)
