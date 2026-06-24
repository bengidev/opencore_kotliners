package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.contextwindow.models.ContextWindowUsage
import io.github.bengidev.opencore.home.speedmode.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {

    private val sampleModel = SidePanelModel(
        id = "openrouter/free",
        displayTitle = "Free Models Router",
        isFree = true,
        contextLength = 200_000
    )

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
    fun modelSelectorTapped_disablesFreeFilterWhenSelectedModelIsPaid() {
        val paidModel = SidePanelModel(id = "openai/gpt-4o", displayTitle = "GPT-4o", isFree = false)
        val result = HomeReducer.reduce(
            HomeState(
                selectedProviderId = SidePanelProviderApi.openRouter.id,
                selectedModelId = paidModel.id,
                availableModels = listOf(sampleModel, paidModel),
                modelFilterFreeOnly = true
            ),
            HomeIntent.ModelSelectorTapped
        )
        assertFalse(result.modelFilterFreeOnly)
    }

    @Test
    fun modelPickerTitle_showsNotAvailableWithoutCatalog() {
        val state = HomeState()
        assertEquals("Not Available", state.modelPickerTitle)
    }

    @Test
    fun modelPickerTitle_usesSelectedTitleWhenCatalogLoaded() {
        val state = HomeState(
            selectedModelId = "openrouter/free",
            selectedModelTitle = "Free Models Router",
            availableModels = listOf(sampleModel)
        )
        assertEquals("Free Models Router", state.modelPickerTitle)
    }

    @Test
    fun modelSelectorTapped_enablesFreeFilterWhenCatalogHasFreeModels() {
        val result = HomeReducer.reduce(
            HomeState(
                availableModels = listOf(sampleModel),
                selectedModelId = sampleModel.id
            ),
            HomeIntent.ModelSelectorTapped
        )
        assertTrue(result.modelFilterFreeOnly)
    }

    @Test
    fun modelSelectorTapped_hidesFreeFilterWhenCatalogHasNoFreeModels() {
        val paidModel = SidePanelModel(id = "openai/gpt-4o", displayTitle = "GPT-4o", isFree = false)
        val result = HomeReducer.reduce(
            HomeState(
                availableModels = listOf(paidModel),
                selectedModelId = paidModel.id
            ),
            HomeIntent.ModelSelectorTapped
        )
        assertFalse(result.modelFilterFreeOnly)
    }

    @Test
    fun catalogCleared_resetsCatalogState() {
        val result = HomeReducer.reduce(
            HomeState(
                availableModels = listOf(sampleModel),
                modelCatalogIsLive = true,
                modelCatalogErrorHint = "error"
            ),
            HomeIntent.CatalogCleared
        )
        assertTrue(result.availableModels.isEmpty())
        assertFalse(result.modelCatalogIsLive)
        assertNull(result.modelCatalogErrorHint)
    }

    @Test
    fun modelSelectionLoaded_clearsSelectionWhenCatalogEmpty() {
        val result = HomeReducer.reduce(
            HomeState(selectedModelId = sampleModel.id),
            HomeIntent.ModelSelectionLoaded(
                modelId = null,
                modelTitle = null,
                providerId = SidePanelProviderApi.openRouter.id,
                models = emptyList()
            )
        )
        assertNull(result.selectedModelId)
        assertEquals("Not Available", result.selectedModelTitle)
    }

    @Test
    fun modelFilterFreeOnlyChanged_filtersModelsInState() {
        val models = listOf(
            sampleModel,
            SidePanelModel(id = "openai/gpt-4o", displayTitle = "GPT-4o", isFree = false)
        )
        val state = HomeState(
            availableModels = models,
            modelFilterFreeOnly = true
        )

        assertEquals(1, state.filteredModels.size)
        assertTrue(state.filteredModels.first().isFree)
    }

    @Test
    fun appliedSearchQuery_filtersByTitleOrId() {
        val models = listOf(
            sampleModel,
            SidePanelModel(
                id = "deepseek/deepseek-r1:free",
                displayTitle = "DeepSeek R1 (free)",
                isFree = true
            )
        )
        val state = HomeState(
            availableModels = models,
            appliedSearchQuery = "deepseek"
        )

        assertEquals(1, state.filteredModels.size)
        assertEquals("deepseek/deepseek-r1:free", state.filteredModels.first().id)
    }

    @Test
    fun modelSelectionLoaded_tracksReasoningSupport() {
        val models = listOf(
            sampleModel,
            SidePanelModel(
                id = "deepseek/deepseek-r1:free",
                displayTitle = "DeepSeek R1 (free)",
                isFree = true,
                supportsReasoning = true
            )
        )
        val result = HomeReducer.reduce(
            HomeState(),
            HomeIntent.ModelSelectionLoaded(
                modelId = "deepseek/deepseek-r1:free",
                modelTitle = "DeepSeek R1 (free)",
                providerId = SidePanelProviderApi.openRouter.id,
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

    @Test
    fun speedModeSelected_updatesSupportedRouterModel() {
        val router = sampleModel.copy(id = "openrouter/free", supportsSpeedModes = true)
        val state = HomeState(
            selectedModelId = router.id,
            selectedModelSupportsSpeedModes = true
        )
        val result = HomeReducer.reduce(state, HomeIntent.SpeedModeSelected(HomeComposerSpeedMode.FAST))
        assertEquals(HomeComposerSpeedMode.FAST, result.speedMode)
        assertEquals("throughput", result.activeProviderSortBy)
    }

    @Test
    fun speedModeSelected_ignoredForUnsupportedModel() {
        val state = HomeState(
            selectedModelId = sampleModel.id,
            selectedModelSupportsSpeedModes = false,
            speedMode = HomeComposerSpeedMode.STANDARD
        )
        val result = HomeReducer.reduce(state, HomeIntent.SpeedModeSelected(HomeComposerSpeedMode.FAST))
        assertEquals(HomeComposerSpeedMode.STANDARD, result.speedMode)
    }

    @Test
    fun modelSelected_resetsUnsupportedSpeedMode() {
        val standardModel = SidePanelModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            displayTitle = "Llama 3.3 70B",
            isFree = true
        )
        val result = HomeReducer.reduce(
            HomeState(speedMode = HomeComposerSpeedMode.FAST),
            HomeIntent.ModelSelected(standardModel)
        )
        assertEquals(HomeComposerSpeedMode.STANDARD, result.speedMode)
        assertFalse(result.selectedModelSupportsSpeedModes)
    }

    @Test
    fun contextUsageUpdated_updatesUsageSnapshot() {
        val usage = ContextWindowUsage(tokensUsed = 102, tokenLimit = 131_072)
        val result = HomeReducer.reduce(HomeState(), HomeIntent.ContextUsageUpdated(usage))
        assertEquals(usage, result.contextUsage)
    }
}
