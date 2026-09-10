package io.github.bengidev.opencore.sidepanel.domain

import kotlin.math.roundToInt

/** User preferences for context window compaction, including auto/manual mode and fill threshold. */
internal data class SettingsContextCompactionPreference(
    val isEnabled: Boolean = true,
    /** Context fill level (percent) at which auto or manual compaction runs. */
    val triggerThresholdPercent: Int = 90,
    val minRecentMessages: Int = 4,
    /** Derived from [triggerThresholdPercent] for trim fallback and planner headroom. */
    val reserveTokens: Int = LEGACY_DEFAULT_RESERVE_TOKENS,
    /** Derived from [triggerThresholdPercent] for planner keep-recent budgeting. */
    val keepRecentTokens: Int = LEGACY_DEFAULT_KEEP_RECENT_TOKENS,
) {
    fun withThresholdPercent(
        percent: Int,
        contextLength: Int = REFERENCE_CONTEXT_LENGTH,
    ): SettingsContextCompactionPreference {
        val clamped = percent.coerceIn(thresholdPercentRange)
        return copy(
            triggerThresholdPercent = clamped,
            reserveTokens = derivedReserveTokens(clamped, contextLength),
            keepRecentTokens = derivedKeepRecentTokens(clamped, contextLength),
        )
    }

    /** Reconciles decoded fields and migrates legacy reserve-token preferences. */
    fun normalizeAfterDecoding(): SettingsContextCompactionPreference {
        val derivedReserve = derivedReserveTokens(triggerThresholdPercent, REFERENCE_CONTEXT_LENGTH)
        val derivedKeep = derivedKeepRecentTokens(triggerThresholdPercent, REFERENCE_CONTEXT_LENGTH)

        val matchesDerived = reserveTokens == derivedReserve && keepRecentTokens == derivedKeep
        val looksLikeLegacyDefaults = reserveTokens == LEGACY_DEFAULT_RESERVE_TOKENS &&
            keepRecentTokens == LEGACY_DEFAULT_KEEP_RECENT_TOKENS

        if (matchesDerived) return this

        if (looksLikeLegacyDefaults) {
            return withThresholdPercent(triggerThresholdPercent)
        }

        val migratedPercent = thresholdPercent(reserveTokens, REFERENCE_CONTEXT_LENGTH)
        return withThresholdPercent(migratedPercent)
    }

    fun scaledReserveTokens(contextLength: Int): Int =
        derivedReserveTokens(triggerThresholdPercent, contextLength)

    fun scaledKeepRecentTokens(contextLength: Int): Int =
        derivedKeepRecentTokens(triggerThresholdPercent, contextLength)

    companion object {
        val thresholdPercentRange = 70..95

        private const val REFERENCE_CONTEXT_LENGTH = 131_072
        private const val TOKEN_STEP = 1_024
        const val LEGACY_DEFAULT_RESERVE_TOKENS = 16_384
        const val LEGACY_DEFAULT_KEEP_RECENT_TOKENS = 20_000

        fun derivedReserveTokens(
            percent: Int,
            contextLength: Int = REFERENCE_CONTEXT_LENGTH,
        ): Int {
            if (contextLength <= 0) return 4_096
            val reservedFraction = (100 - percent) / 100.0
            val raw = contextLength * reservedFraction
            return snapTokenCount(raw.roundToInt(), 4_096..32_768)
        }

        fun derivedKeepRecentTokens(
            percent: Int,
            contextLength: Int = REFERENCE_CONTEXT_LENGTH,
        ): Int {
            val raw = derivedReserveTokens(percent, contextLength) * 1.5
            return snapTokenCount(raw.roundToInt(), 4_096..40_960)
        }

        fun thresholdPercent(reserveTokens: Int, contextLength: Int): Int {
            if (contextLength <= 0) return 90
            val raw = 100 - (reserveTokens.toDouble() / contextLength * 100).roundToInt()
            return raw.coerceIn(thresholdPercentRange)
        }

        private fun snapTokenCount(value: Int, range: IntRange): Int {
            val stepped = maxOf(range.first, ((value + TOKEN_STEP / 2) / TOKEN_STEP) * TOKEN_STEP)
            return minOf(range.last, stepped)
        }
    }
}
