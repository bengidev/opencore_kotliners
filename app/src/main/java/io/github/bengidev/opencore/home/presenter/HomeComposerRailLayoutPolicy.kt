package io.github.bengidev.opencore.home.presenter

internal enum class HomeComposerRailControl {
    REASONING,
    SPEED,
    CONTEXT_USAGE,
}

internal data class HomeComposerRailLayout(
    val modelUsesFlexibleWidth: Boolean,
    val modelFillsFlexibleWidth: Boolean,
    val prioritizedControls: List<HomeComposerRailControl>,
)

internal object HomeComposerRailLayoutPolicy {
    fun layout(
        hasReasoning: Boolean,
        hasSpeed: Boolean,
    ): HomeComposerRailLayout = HomeComposerRailLayout(
        modelUsesFlexibleWidth = true,
        modelFillsFlexibleWidth = false,
        prioritizedControls = buildList {
            if (hasReasoning) add(HomeComposerRailControl.REASONING)
            if (hasSpeed) add(HomeComposerRailControl.SPEED)
            add(HomeComposerRailControl.CONTEXT_USAGE)
        },
    )
}
