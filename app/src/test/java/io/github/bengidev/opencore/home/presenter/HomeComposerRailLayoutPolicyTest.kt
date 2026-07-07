package io.github.bengidev.opencore.home.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeComposerRailLayoutPolicyTest {

    @Test
    fun layout_capsModelToRemainingWidthWithoutForcingItToFill() {
        val layout = HomeComposerRailLayoutPolicy.layout(
            hasReasoning = true,
            hasSpeed = true,
        )

        assertTrue(layout.modelUsesFlexibleWidth)
        assertFalse(layout.modelFillsFlexibleWidth)
        assertEquals(
            listOf(
                HomeComposerRailControl.REASONING,
                HomeComposerRailControl.SPEED,
                HomeComposerRailControl.CONTEXT_USAGE,
            ),
            layout.prioritizedControls,
        )
    }
}
