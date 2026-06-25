package io.github.bengidev.opencore.home.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeComposerSpeedModeTest {

    @Test
    fun fastModeProviderSort() {
        assertEquals("throughput", HomeComposerSpeedMode.FAST.providerSortBy)
        assertNull(HomeComposerSpeedMode.STANDARD.providerSortBy)
    }

    @Test
    fun titlesMatchSwiftLabels() {
        assertEquals("Standard", HomeComposerSpeedMode.STANDARD.title)
        assertEquals("Fast", HomeComposerSpeedMode.FAST.title)
    }
}
