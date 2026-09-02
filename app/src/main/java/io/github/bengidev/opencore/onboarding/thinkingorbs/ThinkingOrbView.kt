package io.github.bengidev.opencore.onboarding.thinkingorbs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun ThinkingOrbView(
    state: OrbState,
    isDark: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    size: OrbSize = OrbSize.PX64,
    speed: Double = 1.0,
    displaySize: Dp = 34.dp,
    paused: Boolean = false
) {
    val density = LocalDensity.current
    val sidePx = with(density) { displaySize.toPx() }
    val preset = remember(state, size) { OrbSpec.resolvePreset(state, size) }
    val effectiveSpeed = preset.speed * speed
    val view = LocalView.current
    val animationClock = remember(state, size, reduceMotion, paused, effectiveSpeed) {
        OrbAnimationClock()
    }

    LaunchedEffect(state, size, reduceMotion, paused, effectiveSpeed) {
        if (reduceMotion || paused) {
            animationClock.freeze(OrbSpec.REDUCED_MOTION_T * effectiveSpeed)
            view.invalidate()
            return@LaunchedEffect
        }
        animationClock.reset()
        while (true) {
            withFrameNanos { frameNanos ->
                animationClock.onFrame(frameNanos, effectiveSpeed)
                view.invalidate()
            }
        }
    }

    Canvas(
        modifier = modifier
            .size(displaySize)
            .semantics { contentDescription = state.label }
    ) {
        val frame = orbFrame(preset, size.value, animationClock.timeSeconds)
        val zoom = sidePx / size.value.toFloat()
        scale(zoom, pivot = Offset.Zero) {
            for (line in frame.lines) {
                drawLine(
                    color = inkColor(isDark, line.white, line.a),
                    start = Offset(line.x1.toFloat(), line.y1.toFloat()),
                    end = Offset(line.x2.toFloat(), line.y2.toFloat()),
                    strokeWidth = line.w.toFloat()
                )
            }
            for (dot in frame.dots) {
                drawCircle(
                    color = inkColor(isDark, dot.white, dot.a),
                    radius = dot.r.toFloat(),
                    center = Offset(dot.x.toFloat(), dot.y.toFloat())
                )
            }
        }
    }
}

private class OrbAnimationClock {
    private var startNanos: Long = 0L
    var timeSeconds: Double = 0.0
        private set

    fun reset() {
        startNanos = 0L
        timeSeconds = 0.0
    }

    fun freeze(t: Double) {
        startNanos = 0L
        timeSeconds = t
    }

    fun onFrame(frameNanos: Long, speed: Double) {
        if (startNanos == 0L) startNanos = frameNanos
        timeSeconds = (frameNanos - startNanos) / 1_000_000_000.0 * speed
    }
}

private fun inkColor(isDark: Boolean, white: Double, alpha: Double): Color {
    val w = white.coerceIn(0.0, 1.0)
    val g = ((if (isDark) 1 - w else w) * 255).roundToInt() / 255f
    return Color(g, g, g, alpha.toFloat().coerceIn(0f, 1f))
}

internal fun thinkingOrbStateForIndex(orbStyleIndex: Int): OrbState {
    val states = listOf(
        OrbState.BREATHING,
        OrbState.COMPOSING,
        OrbState.SHAPING,
        OrbState.WORKING
    )
    return states[orbStyleIndex % states.size]
}
