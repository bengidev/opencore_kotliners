package io.github.bengidev.opencore.sidepanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidePanelModelCatalogTest {

    @Test
    fun modelsFor_openRouter_marksFallbackModelsAsFree() {
        val models = SidePanelModelCatalog.modelsFor(SidePanelProviderApi.openRouter)
        assertTrue(models.any { it.displayTitle == "Free Models Router" })
        assertTrue(models.all { it.isFree })
    }

    @Test
    fun modelsFor_openRouter_enablesSpeedModesOnFreeRouter() {
        val router = SidePanelModelCatalog.modelsFor(SidePanelProviderApi.openRouter)
            .first { it.id == "openrouter/free" }
        assertTrue(router.supportsSpeedModes)
    }

    @Test
    fun displayTitle_resolvesKnownModel() {
        val title = SidePanelModelCatalog.displayTitle(
            providerId = SidePanelProviderApi.openRouter.id,
            modelId = "openrouter/free"
        )
        assertEquals("Free Models Router", title)
    }

    @Test
    fun defaultModel_isFirstCatalogEntry() {
        val default = SidePanelModelCatalog.defaultModel(SidePanelProviderApi.openRouter)
        assertEquals(SidePanelModelCatalog.modelsFor(SidePanelProviderApi.openRouter).first(), default)
    }
}
