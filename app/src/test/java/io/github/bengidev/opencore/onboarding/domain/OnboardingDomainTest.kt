package io.github.bengidev.opencore.onboarding.domain

import io.github.bengidev.opencore.onboarding.presenter.HeroCubeLayoutMath
import io.github.bengidev.opencore.onboarding.presenter.carousel.cardStepOffset
import io.github.bengidev.opencore.onboarding.presenter.carousel.modularRelative
import io.github.bengidev.opencore.onboarding.presenter.carousel.normalizeScrollIndex
import io.github.bengidev.opencore.onboarding.presenter.carousel.wrappedIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.dp

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

    @Test
    fun carouselMath_cardStepIncludesGap() {
        val step = cardStepOffset(1f, 300.dp)
        assertEquals(314f, step.value, 0.01f)
    }

    @Test
    fun carouselMath_wrapsIndices() {
        assertEquals(1, wrappedIndex(5, 4))
        assertEquals(0f, modularRelative(0, 4f, 4), 0.01f)
        assertEquals(2f, normalizeScrollIndex(6f, 4), 0.01f)
    }
}
