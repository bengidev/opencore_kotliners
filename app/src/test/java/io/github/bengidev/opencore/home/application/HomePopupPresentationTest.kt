package io.github.bengidev.opencore.home.application

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
}
