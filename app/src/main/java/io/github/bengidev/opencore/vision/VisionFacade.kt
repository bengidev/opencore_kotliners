package io.github.bengidev.opencore.vision

import android.content.Context
import io.github.bengidev.opencore.vision.application.VisionFlowController

internal class VisionFacade {
    fun createController(context: Context): VisionFlowController =
        VisionFlowController(context = context)
}
