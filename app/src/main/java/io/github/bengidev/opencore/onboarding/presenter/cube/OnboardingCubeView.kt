package io.github.bengidev.opencore.onboarding.presenter.cube

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingCubeView(
    appeared: Boolean,
    inkColor: Color,
    morphPaused: Boolean,
    rotationProgress: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val renderer = remember { CubeRenderer() }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    var animationEpochNanos by remember { mutableLongStateOf(0L) }
    var hasAnimationEpoch by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val vertexRadiusPx = with(density) { 3.5.dp.toPx() }

    SideEffect {
        renderer.setAppeared(appeared)
        renderer.setMorphPaused(morphPaused)
        renderer.setRotationProgress(rotationProgress)
        renderer.setReduceMotion(reduceMotion)
    }

    LaunchedEffect(appeared, reduceMotion) {
        if (!appeared) {
            hasAnimationEpoch = false
            frameTimeNanos = 0L
            return@LaunchedEffect
        }
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            withFrameNanos { frameTime ->
                if (!hasAnimationEpoch) {
                    animationEpochNanos = frameTime
                    hasAnimationEpoch = true
                }
                frameTimeNanos = frameTime
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
    ) {
        if (!appeared) return@Canvas

        val elapsedSeconds = if (reduceMotion || !hasAnimationEpoch) {
            0.0
        } else {
            (frameTimeNanos - animationEpochNanos) / 1_000_000_000.0
        }

        val frame = renderer.tick(elapsedSeconds, size.width, size.height)
        val strokeWidth = strokeWidthPx
        val vertexRadius = vertexRadiusPx
        val dashOn = with(density) { 4.dp.toPx() }
        val dashOff = with(density) { 3.dp.toPx() }
        val dashedPathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f)

        frame.edges.forEachIndexed { index, edge ->
            val start = frame.projectedVertices[edge.start]
            val end = frame.projectedVertices[edge.end]
            val strokeEnd = frame.edgeStrokeEnds[index].coerceIn(0f, 1f)
            if (strokeEnd <= 0f) return@forEachIndexed

            val trimmedEnd = Offset(
                start.x + (end.x - start.x) * strokeEnd,
                start.y + (end.y - start.y) * strokeEnd
            )

            val path = if (strokeEnd >= 1f) {
                Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(end.x, end.y)
                }
            } else {
                Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(trimmedEnd.x, trimmedEnd.y)
                }
            }

            val pathEffect = if (frame.dashedEdgeIndices.contains(index)) {
                dashedPathEffect
            } else {
                null
            }

            drawPath(
                path = path,
                color = inkColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect
                )
            )
        }

        frame.projectedVertices.forEachIndexed { index, point ->
            val opacity = frame.vertexOpacities[index]
            if (opacity <= 0f) return@forEachIndexed
            val scale = 0.85f + 0.15f * opacity
            drawCircle(
                color = inkColor.copy(alpha = opacity),
                radius = vertexRadius * scale,
                center = point
            )
        }
    }
}
