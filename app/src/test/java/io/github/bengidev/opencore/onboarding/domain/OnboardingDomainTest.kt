package io.github.bengidev.opencore.onboarding.domain

import io.github.bengidev.opencore.onboarding.presenter.HeroCubeLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDomainTest {

    @Test
    fun onboardingFeature_catalog_hasFourFeaturesInOrder() {
        assertEquals(4, OnboardingFeature.catalog.size)
        assertEquals("neural_core", OnboardingFeature.catalog[0].id)
        assertEquals("spatial_canvas", OnboardingFeature.catalog[1].id)
        assertEquals("workflows", OnboardingFeature.catalog[2].id)
        assertEquals("vault", OnboardingFeature.catalog[3].id)
    }

    @Test
    fun onboardingFeature_hasUniqueIds() {
        val ids = OnboardingFeature.catalog.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun onboardingFeature_includesChatPrompts() {
        val neural = OnboardingFeature.catalog.first { it.id == "neural_core" }
        assertEquals("How does on-device reasoning work?", neural.userPrompt)
        assertEquals("cpu", neural.iconName)
    }

    @Test
    fun onboardingChatMessage_thinkingPreparesAssistantPayload() {
        val feature = OnboardingFeature.catalog.first()
        val thinking = OnboardingChatMessage.thinking(feature)
        val prepared = thinking.preparedAssistant

        requireNotNull(prepared)
        assertEquals(thinking.id, prepared.id)
        assertEquals(OnboardingChatRole.ASSISTANT, prepared.role)
        assertEquals(feature.accessibilitySummary, prepared.text)
    }

    @Test
    fun onboardingChatMessage_morphsThinkingToAssistant() {
        val feature = OnboardingFeature.catalog.first()
        val thinking = OnboardingChatMessage.thinking(feature)
        val assistant = thinking.morphToAssistant()

        assertEquals(thinking.id, assistant.id)
        assertEquals(OnboardingChatRole.ASSISTANT, assistant.role)
        assertEquals(feature.accessibilitySummary, assistant.text)
    }

    @Test
    fun onboardingFeature_wrapsCatalogIndex() {
        assertEquals(1, OnboardingFeature.wrappedCatalogIndex(5))
        assertEquals(0, OnboardingFeature.wrappedCatalogIndex(4))
    }

    @Test
    fun heroCubeLayout_morphsFromCenterToHeader() {
        val large = HeroCubeLayoutMath.layoutForMorph(
            morph = 0f,
            width = 400f,
            height = 800f,
            largeSize = 220f,
            smallSize = 36f,
            headerTopPadding = 18f,
            headerHorizontalPadding = 24f,
            safeTop = 0f
        )
        val small = HeroCubeLayoutMath.layoutForMorph(
            morph = 1f,
            width = 400f,
            height = 800f,
            largeSize = 220f,
            smallSize = 36f,
            headerTopPadding = 18f,
            headerHorizontalPadding = 24f,
            safeTop = 0f
        )

        assertEquals(220f, large.size, 0.01f)
        assertEquals(36f, small.size, 0.01f)
        assertEquals(200f, large.center.x, 0.01f)
        assertTrue(small.center.y < large.center.y)
    }
}
