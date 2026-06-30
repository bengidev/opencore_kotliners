package io.github.bengidev.opencore.vision.application

internal data class VisionFlowState(
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)
