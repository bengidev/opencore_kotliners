package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePopupPresentationTest {

    @Test
    fun openingContextUsage_closesModelPicker() {
        val result = HomeReducer.reduce(
            HomeState(isModelPickerVisible = true),
            HomeIntent.ContextUsagePresentedChanged(presented = true),
        )
        assertTrue(result.isContextUsagePresented)
        assertFalse(result.isModelPickerVisible)
    }

    @Test
    fun openingModelPicker_closesContextUsage() {
        val result = HomeReducer.reduce(
            HomeState(isContextUsagePresented = true),
            HomeIntent.ModelSelectorTapped,
        )
        assertTrue(result.isModelPickerVisible)
        assertFalse(result.isContextUsagePresented)
    }

    @Test
    fun sendTapped_closesContextUsage() {
        val model = SidePanelModel(
            id = "openai/gpt-4o-mini",
            displayTitle = "GPT-4o mini",
            isFree = true,
        )
        val result = HomeReducer.reduce(
            HomeState(
                draftMessage = "Hello",
                hasApiKey = true,
                selectedModelId = model.id,
                availableModels = listOf(model),
                isContextUsagePresented = true,
            ),
            HomeIntent.SendTapped,
        )
        assertFalse(result.isContextUsagePresented)
        assertEquals("", result.draftMessage)
    }
}
