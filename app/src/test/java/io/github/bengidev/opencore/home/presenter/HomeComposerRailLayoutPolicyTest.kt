package io.github.bengidev.opencore.home.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeComposerRailLayoutPolicyTest {

    @Test
    fun layout_capsModelToRemainingWidthWithoutForcingItToFill() {
        val layout = HomeComposerRailLayoutPolicy.layout(
            hasReasoning = true,
            hasSpeed = true,
        )

        assertFalse(layout.modelFillsFlexibleWidth)
        assertEquals(
            listOf(
                HomeComposerRailControl.REASONING,
                HomeComposerRailControl.SPEED,
                HomeComposerRailControl.CONTEXT_USAGE,
            ),
            layout.prioritizedControls,
        )
        assertEquals(184, HomeComposerRailLayoutPolicy.reservedControlsWidth(layout).value.toInt())
    }

    @Test
    fun layout_omitsOptionalControlsWhenUnsupported() {
        val layout = HomeComposerRailLayoutPolicy.layout(
            hasReasoning = false,
            hasSpeed = false,
        )

        assertEquals(listOf(HomeComposerRailControl.CONTEXT_USAGE), layout.prioritizedControls)
        assertEquals(38, HomeComposerRailLayoutPolicy.reservedControlsWidth(layout).value.toInt())
    }
}
