package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelSelectionPolicyTest {

    private val freeModel = SidePanelModel(
        id = "meta-llama/llama-3.3-70b-instruct:free",
        displayTitle = "Llama 3.3 70B"
    )
    private val paidModel = SidePanelModel(
        id = "openai/gpt-4o",
        displayTitle = "GPT-4o"
    )

    @Test
    fun reconcile_emptyCatalog_clearsSelection() {
        val result = ModelSelectionPolicy.reconcile(
            models = emptyList(),
            savedModelId = freeModel.id,
            autoSelectWhenNoneSaved = true
        )

        assertNull(result.modelId)
        assertNull(result.modelTitle)
    }

    @Test
    fun reconcile_savedModelInCatalog_keepsSavedModel() {
        val result = ModelSelectionPolicy.reconcile(
            models = listOf(freeModel, paidModel),
            savedModelId = paidModel.id,
            autoSelectWhenNoneSaved = false
        )

        assertEquals(paidModel.id, result.modelId)
        assertEquals(paidModel.displayTitle, result.modelTitle)
    }

    @Test
    fun reconcile_staleSavedModel_replacesWithFirst() {
        val result = ModelSelectionPolicy.reconcile(
            models = listOf(freeModel, paidModel),
            savedModelId = "stale/model-id",
            autoSelectWhenNoneSaved = false
        )

        assertEquals(freeModel.id, result.modelId)
    }

    @Test
    fun reconcile_noSavedModel_autoSelectDisabled_staysUnselected() {
        val result = ModelSelectionPolicy.reconcile(
            models = listOf(freeModel, paidModel),
            savedModelId = null,
            autoSelectWhenNoneSaved = false
        )

        assertNull(result.modelId)
        assertNull(result.modelTitle)
    }

    @Test
    fun reconcile_noSavedModel_autoSelectEnabled_picksFirst() {
        val result = ModelSelectionPolicy.reconcile(
            models = listOf(freeModel, paidModel),
            savedModelId = null,
            autoSelectWhenNoneSaved = true
        )

        assertEquals(freeModel.id, result.modelId)
    }
}
