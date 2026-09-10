package io.github.bengidev.opencore.sidepanel.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsContextCompactionPreferenceTest {

    @Test
    fun defaultPreference_usesNinetyPercentThresholdWithDerivedTokens() {
        val preference = SettingsContextCompactionPreference().normalizeAfterDecoding()
        assertEquals(90, preference.triggerThresholdPercent)
        assertEquals(
            SettingsContextCompactionPreference.derivedReserveTokens(90),
            preference.reserveTokens,
        )
        assertEquals(
            SettingsContextCompactionPreference.derivedKeepRecentTokens(90),
            preference.keepRecentTokens,
        )
    }

    @Test
    fun withThresholdPercent_updatesDerivedTokenBudgets() {
        val preference = SettingsContextCompactionPreference().withThresholdPercent(75)
        assertEquals(75, preference.triggerThresholdPercent)
        assertEquals(
            SettingsContextCompactionPreference.derivedReserveTokens(75),
            preference.reserveTokens,
        )
        assertEquals(
            SettingsContextCompactionPreference.derivedKeepRecentTokens(75),
            preference.keepRecentTokens,
        )
    }

    @Test
    fun normalizeAfterDecoding_migratesLegacyReserveTokens() {
        val legacy = SettingsContextCompactionPreference(
            triggerThresholdPercent = 90,
            reserveTokens = 4_096,
            keepRecentTokens = 20_000,
        )
        val migrated = legacy.normalizeAfterDecoding()
        assertEquals(95, migrated.triggerThresholdPercent)
        assertEquals(
            SettingsContextCompactionPreference.derivedReserveTokens(95),
            migrated.reserveTokens,
        )
    }
}
