package io.github.bengidev.opencore.onboarding.thinkingorbs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class OrbDot(
    var x: Double,
    var y: Double,
    var z: Double,
    var r: Double,
    var white: Double,
    var a: Double = 1.0
)

internal data class OrbLine(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val white: Double,
    val a: Double,
    val w: Double
)

internal data class OrbFrame(
    val dots: List<OrbDot>,
    val lines: List<OrbLine>
)

internal fun hashD(a: Double, b: Double): Double {
    val h = sin(a * 12.9898 + b * 78.233) * 43_758.5453
    return h - floor(h)
}

internal fun vnoise(x: Double, y: Double): Double {
    val xi = floor(x)
    val yi = floor(y)
    var fx = x - xi
    var fy = y - yi
    fx = fx * fx * (3 - 2 * fx)
    fy = fy * fy * (3 - 2 * fy)
    val a = hashD(xi, yi)
    val b = hashD(xi + 1, yi)
    val c = hashD(xi, yi + 1)
    val d = hashD(xi + 1, yi + 1)
    return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy
}

internal fun fibDir(i: Int, n: Int): Triple<Double, Double, Double> {
    val golden = PI * (3 - sqrt(5.0))
    val y = 1 - (2 * (i + 0.5)) / n
    val rad = sqrt(1 - y * y)
    val a = i * golden
    return Triple(rad * cos(a), y, rad * sin(a))
}

internal fun angleDelta(a: Double, b: Double): Double = atan2(sin(a - b), cos(a - b))

internal fun lerp(a: Double, b: Double, f: Double): Double = a + (b - a) * f

internal fun frac(x: Double): Double = x - floor(x)

internal class Projector(
    yaw: Double,
    tilt: Double,
    private val cx: Double,
    private val cy: Double,
    private val scale: Double
) {
    private val st = sin(tilt)
    private val ct = cos(tilt)
    private val sy = sin(yaw)
    private val cyw = cos(yaw)

    operator fun invoke(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        val x1 = x * cyw + z * sy
        val z1 = -x * sy + z * cyw
        val y1 = y * ct - z1 * st
        val z2 = y * st + z1 * ct
        return Triple(cx + x1 * scale, cy - y1 * scale, z2)
    }
}

internal fun radiusScale(size: Double, pow: Double): Double = (size / 300.0).pow(pow)

internal fun finalizeFrame(dots: List<OrbDot>, lines: List<OrbLine>, rMin: Double = 0.3): OrbFrame {
    val visible = dots.mapIndexedNotNull { index, dot ->
        if (dot.a < 0.02) return@mapIndexedNotNull null
        dot.copy(r = max(rMin, dot.r))
    }
    val sorted = visible
        .withIndex()
        .sortedWith(compareBy<IndexedValue<OrbDot>> { it.value.z }.thenBy { it.index })
        .map { it.value }
    return OrbFrame(
        dots = sorted,
        lines = lines.filter { it.a >= 0.02 }
    )
}

internal fun Double.truncatingRemainder(divisor: Double): Double {
    if (divisor == 0.0) return this
    return this - (this / divisor).toLong() * divisor
}
