package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidePanelSettingReducerTest {

    @Test
    fun draftChanged_updatesDraftAndClearsError() {
        val result = SidePanelSettingReducer.reduce(
            SidePanelSettingState(errorMessage = "oops"),
            SidePanelSettingIntent.DraftChanged("sk-test")
        )
        assertEquals("sk-test", result.draftApiKey)
        assertEquals(null, result.errorMessage)
        assertTrue(result.canSave)
    }

    @Test
    fun saveSucceeded_clearsDraftAndMarksStored() {
        val result = SidePanelSettingReducer.reduce(
            SidePanelSettingState(draftApiKey = "sk-test"),
            SidePanelSettingIntent.SaveSucceeded
        )
        assertEquals("", result.draftApiKey)
        assertTrue(result.hasStoredKey)
    }

    @Test
    fun compactionEnabledChanged_updatesPreference() {
        val preference = SettingsContextCompactionPreference(isEnabled = false, reserveTokens = 8_192)
        val result = SidePanelSettingReducer.reduce(
            SidePanelSettingState(compactionPreference = preference),
            SidePanelSettingIntent.CompactionEnabledChanged(true),
        )
        assertTrue(result.compactionPreference.isEnabled)
        assertEquals(8_192, result.compactionPreference.reserveTokens)
    }

    @Test
    fun reserveTokensChanged_updatesPreference() {
        val result = SidePanelSettingReducer.reduce(
            SidePanelSettingState(),
            SidePanelSettingIntent.ReserveTokensChanged(12_288),
        )
        assertEquals(12_288, result.compactionPreference.reserveTokens)
    }
}
