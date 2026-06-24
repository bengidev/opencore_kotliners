package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {

    private val sampleModel = SidePanelModel(id = "openrouter/free", displayTitle = "Free Models Router")

    @Test
    fun draftMessageChanged_updatesDraft() {
        val result = HomeReducer.reduce(
            HomeState(selectedModelId = sampleModel.id, hasApiKey = true),
            HomeIntent.DraftMessageChanged("Hello")
        )
        assertEquals("Hello", result.draftMessage)
        assertTrue(result.canSend)
    }

    @Test
    fun canSend_requiresSelectedModel() {
        assertFalse(HomeState(draftMessage = "Hello").canSend)
        assertFalse(
            HomeState(
                draftMessage = "Hello",
                selectedModelId = sampleModel.id,
                hasLoadedCredentials = true
            ).canSend
        )
        assertTrue(
            HomeState(
                draftMessage = "Hello",
                selectedModelId = sampleModel.id,
                hasApiKey = true
            ).canSend
        )
    }

    @Test
    fun showMissingApiKeyHint_requiresLoadedCredentialsWithoutKey() {
        assertFalse(HomeState(hasLoadedCredentials = false, hasApiKey = false).showMissingApiKeyHint)
        assertTrue(HomeState(hasLoadedCredentials = true, hasApiKey = false).showMissingApiKeyHint)
        assertFalse(HomeState(hasLoadedCredentials = true, hasApiKey = true).showMissingApiKeyHint)
    }

    @Test
    fun credentialsLoaded_updatesApiKeyState() {
        val result = HomeReducer.reduce(
            HomeState(),
            HomeIntent.CredentialsLoaded(hasApiKey = true)
        )
        assertTrue(result.hasApiKey)
        assertTrue(result.hasLoadedCredentials)
    }

    @Test
    fun sendTapped_clearsDraftWhenNotBlank() {
        val result = HomeReducer.reduce(
            HomeState(
                draftMessage = "Hello",
                selectedModelId = sampleModel.id,
                hasApiKey = true
            ),
            HomeIntent.SendTapped
        )
        assertEquals("", result.draftMessage)
        assertFalse(result.canSend)
    }

    @Test
    fun sendTapped_isNoOpWhenDraftBlank() {
        val state = HomeState(draftMessage = "   ", selectedModelId = sampleModel.id)
        val result = HomeReducer.reduce(state, HomeIntent.SendTapped)
        assertEquals(state, result)
    }

    @Test
    fun modelSelectorTapped_showsPicker() {
        val result = HomeReducer.reduce(HomeState(), HomeIntent.ModelSelectorTapped)
        assertTrue(result.isModelPickerVisible)
    }

    @Test
    fun modelSelected_updatesSelectionAndHidesPicker() {
        val result = HomeReducer.reduce(
            HomeState(isModelPickerVisible = true),
            HomeIntent.ModelSelected(sampleModel)
        )
        assertEquals(sampleModel.id, result.selectedModelId)
        assertEquals(sampleModel.displayTitle, result.selectedModelTitle)
        assertFalse(result.selectedModelSupportsReasoning)
        assertFalse(result.isModelPickerVisible)
    }

    @Test
    fun modelSelected_tracksReasoningSupport() {
        val reasoningModel = SidePanelModel(
            id = "deepseek/deepseek-r1:free",
            displayTitle = "DeepSeek R1 (free)",
            supportsReasoning = true
        )
        val result = HomeReducer.reduce(
            HomeState(),
            HomeIntent.ModelSelected(reasoningModel)
        )
        assertTrue(result.selectedModelSupportsReasoning)
    }

    @Test
    fun modelSelectionLoaded_tracksReasoningSupport() {
        val models = listOf(
            sampleModel,
            SidePanelModel(
                id = "deepseek/deepseek-r1:free",
                displayTitle = "DeepSeek R1 (free)",
                supportsReasoning = true
            )
        )
        val result = HomeReducer.reduce(
            HomeState(),
            HomeIntent.ModelSelectionLoaded(
                modelId = "deepseek/deepseek-r1:free",
                modelTitle = "DeepSeek R1 (free)",
                models = models
            )
        )
        assertTrue(result.selectedModelSupportsReasoning)
    }

    @Test
    fun placeholderIntents_areNoOps() {
        val state = HomeState(draftMessage = "Draft", selectedModelId = sampleModel.id)
        val intents = listOf(
            HomeIntent.AttachmentTapped,
            HomeIntent.ContextUsageTapped,
            HomeIntent.MicrophoneTapped,
            HomeIntent.NewConversationTapped,
            HomeIntent.SidebarTapped,
            HomeIntent.SpeedModeTapped
        )

        intents.forEach { intent ->
            assertEquals(state, HomeReducer.reduce(state, intent))
        }
    }
}
