package io.github.bengidev.opencore.home.presenter

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class HomeComposerRailControl {
    REASONING,
    SPEED,
    CONTEXT_USAGE,
}

internal data class HomeComposerRailLayout(
    val modelFillsFlexibleWidth: Boolean,
    val prioritizedControls: List<HomeComposerRailControl>,
)

internal object HomeComposerRailLayoutPolicy {
    private val reasoningChipWidth = 92.dp
    private val iconChipWidth = 38.dp
    private val chipSpacing = 8.dp

    fun layout(
        hasReasoning: Boolean,
        hasSpeed: Boolean,
    ): HomeComposerRailLayout = HomeComposerRailLayout(
        modelFillsFlexibleWidth = false,
        prioritizedControls = buildList {
            if (hasReasoning) add(HomeComposerRailControl.REASONING)
            if (hasSpeed) add(HomeComposerRailControl.SPEED)
            add(HomeComposerRailControl.CONTEXT_USAGE)
        },
    )

    fun reservedControlsWidth(layout: HomeComposerRailLayout): Dp {
        if (layout.prioritizedControls.isEmpty()) return 0.dp
        var width = 0.dp
        layout.prioritizedControls.forEachIndexed { index, control ->
            if (index > 0) width += chipSpacing
            width += when (control) {
                HomeComposerRailControl.REASONING -> reasoningChipWidth
                HomeComposerRailControl.SPEED, HomeComposerRailControl.CONTEXT_USAGE -> iconChipWidth
            }
        }
        return width
    }
}
