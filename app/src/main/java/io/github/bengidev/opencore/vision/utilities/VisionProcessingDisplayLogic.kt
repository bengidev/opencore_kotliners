package io.github.bengidev.opencore.vision.utilities

import io.github.bengidev.opencore.vision.domain.VisionMediaKind

internal object VisionProcessingDisplayLogic {
    fun statusMessage(kind: VisionMediaKind): String = when (kind) {
        VisionMediaKind.IMAGE -> "Preparing photo…"
        VisionMediaKind.VIDEO -> "Preparing video…"
        VisionMediaKind.PLAIN_TEXT -> "Reading file…"
        VisionMediaKind.UNSUPPORTED -> "Preparing attachment…"
    }
}
