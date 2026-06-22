package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
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
    fun reasoningModelSelected_updatesModel() {
        val result = SidePanelSettingReducer.reduce(
            SidePanelSettingState(),
            SidePanelSettingIntent.ReasoningModelSelected(SidePanelReasoningModel.Low)
        )
        assertEquals(SidePanelReasoningModel.Low, result.reasoningModel)
    }

    @Test
    fun blankDraftCannotSave() {
        assertFalse(SidePanelSettingState(draftApiKey = "   ").canSave)
    }
}
