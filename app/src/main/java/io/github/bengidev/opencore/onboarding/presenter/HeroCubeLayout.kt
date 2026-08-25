package io.github.bengidev.opencore.onboarding.presenter

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

internal data class HeroCubeLayout(
    val size: Float,
    val center: Offset
)

internal fun Modifier.heroCubeOverlay(layout: HeroCubeLayout): Modifier = this.layout { measurable, constraints ->
    val sizePx = layout.size.coerceAtLeast(1f)
    val measurePx = sizePx.toInt().coerceAtLeast(1)
    val sizeScale = sizePx / measurePx
    val placeable = measurable.measure(Constraints.fixed(measurePx, measurePx))
    val left = layout.center.x - layout.size / 2f
    val top = layout.center.y - layout.size / 2f
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(
            x = left.toInt(),
            y = top.toInt(),
            layerBlock = {
                scaleX = sizeScale
                scaleY = sizeScale
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = left - left.toInt()
                translationY = top - top.toInt()
            }
        )
    }
}

internal object HeroCubeLayoutMath {
    const val HERO_ROTATION_END_TRANSITION = 0.38f
    const val HERO_MORPH_START_TRANSITION = 0.54f

    fun heroRotationProgress(transition: Float): Float {
        val raw = (transition / HERO_ROTATION_END_TRANSITION).coerceIn(0f, 1f)
        return 1f - (1f - raw) * (1f - raw)
    }

    fun heroMorphProgress(transition: Float): Float {
        val span = (1f - HERO_MORPH_START_TRANSITION).coerceAtLeast(0.001f)
        val raw = ((transition - HERO_MORPH_START_TRANSITION) / span).coerceIn(0f, 1f)
        return 1f - (1f - raw) * (1f - raw) * (1f - raw)
    }

    fun layoutForTransition(
        transition: Float,
        width: Float,
        height: Float,
        largeSize: Float,
        smallSize: Float,
        headerTopPadding: Float,
        headerHorizontalPadding: Float,
        safeTop: Float = 0f
    ): HeroCubeLayout = layoutForMorph(
        morph = heroMorphProgress(transition),
        width = width,
        height = height,
        largeSize = largeSize,
        smallSize = smallSize,
        headerTopPadding = headerTopPadding,
        headerHorizontalPadding = headerHorizontalPadding,
        safeTop = safeTop
    )

    fun layoutForMorph(
        morph: Float,
        width: Float,
        height: Float,
        largeSize: Float,
        smallSize: Float,
        headerTopPadding: Float,
        headerHorizontalPadding: Float,
        safeTop: Float
    ): HeroCubeLayout {
        val clampedMorph = morph.coerceIn(0f, 1f)
        val cubeSize = largeSize + (smallSize - largeSize) * clampedMorph

        val largeCenter = Offset(width * 0.5f, height * 0.5f)
        val smallCenter = Offset(
            headerHorizontalPadding + smallSize * 0.5f,
            safeTop + headerTopPadding + smallSize * 0.5f
        )

        return HeroCubeLayout(
            size = cubeSize,
            center = Offset(
                largeCenter.x + (smallCenter.x - largeCenter.x) * clampedMorph,
                largeCenter.y + (smallCenter.y - largeCenter.y) * clampedMorph
            )
        )
    }
}
