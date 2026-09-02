package io.github.bengidev.opencore.onboarding.thinkingorbs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class Move(val axis: Int, val lo: Double, val hi: Double, val ang: Double)

private data class SolveCycle(val amount: List<Double>, val active: Int)

private fun solveCycle(time: Double, count: Int, slotDur: Double, rest: Double): SolveCycle {
    val cyc = 2 * count * slotDur + rest
    val tc = time.truncatingRemainder(cyc)
    val amount = MutableList(count) { 0.0 }
    var active = -1
    if (tc < 2 * count * slotDur) {
        val slot = floor(tc / slotDur).toInt()
        val p = (tc - slot * slotDur) / slotDur
        val cl = min(1.0, p / 0.7)
        val ep = 1 - (1 - cl).pow(3)
        if (slot < count) {
            for (i in 0 until slot) amount[i] = 1.0
            amount[slot] = ep
            active = slot
        } else {
            val u = 2 * count - 1 - slot
            for (i in 0 until u) amount[i] = 1.0
            amount[u] = 1 - ep
            active = u
        }
    }
    return SolveCycle(amount, active)
}

private fun applyMoves(
    point: Triple<Double, Double, Double>,
    moves: List<Move>,
    sc: SolveCycle
): Quadruple<Double, Double, Double, Boolean> {
    var (x, y, z) = point
    var inActive = false
    for (i in moves.indices) {
        if (sc.amount[i] <= 0) continue
        val mv = moves[i]
        val coord = when (mv.axis) {
            0 -> x
            1 -> y
            else -> z
        }
        if (coord < mv.lo || coord >= mv.hi) continue
        if (i == sc.active) inActive = true
        val a = mv.ang * sc.amount[i]
        val ca = cos(a)
        val sa = sin(a)
        when (mv.axis) {
            0 -> {
                val y2 = y * ca - z * sa
                z = y * sa + z * ca
                y = y2
            }
            1 -> {
                val x2 = x * ca + z * sa
                z = -x * sa + z * ca
                x = x2
            }
            else -> {
                val x2 = x * ca - y * sa
                y = x * sa + y * ca
                x = x2
            }
        }
    }
    return Quadruple(x, y, z, inActive)
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun makeMoves(count: Int): List<Move> = List(count) { i ->
    val axis = min(2, floor(hashD(i.toDouble(), 2.3) * 3).toInt())
    val lo = -1.0 + 0.5 * min(3, floor(hashD(i.toDouble(), 5.9) * 4).toInt())
    val dir = if (hashD(i.toDouble(), 7.7) < 0.5) 1.0 else -1.0
    Move(axis, lo, lo + 0.5, dir * PI / 2)
}

internal fun frameGlobe(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val spin = 0.5
    val cx = size / 2
    val cy = size / 2
    val radius = (size / 2) * 0.82
    val tilt = 0.4 + 0.06 * sin(t * 0.35)
    val pt = Projector(yaw = t * spin, tilt = tilt, cx = cx, cy = cy, scale = radius)
    val scan = t * (spin + (1.7 - spin) * (o["scanMul"] ?: 1.0))
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)
    val dimBase = o["dimBase"] ?: 1.0

    val dots = mutableListOf<OrbDot>()
    val latRings = (o["latRings"] ?: 17.0).toInt()
    val lonDensity = o["lonDensity"] ?: 44.0
    for (li in 0..latRings) {
        val lat = -PI / 2 + (li.toDouble() / latRings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val lonCount = max(1, (abs(cosLat) * lonDensity).roundToInt())
        for (lj in 0 until lonCount) {
            val lon = (lj.toDouble() / lonCount) * 2 * PI
            val (px, py, z) = pt(cosLat * cos(lon), sinLat, cosLat * sin(lon))
            val depth = (z + 1) / 2
            val d = angleDelta(lon + t * spin, scan)
            val boost = exp(-(d * d) / 0.18) * max(0.0, z)
            dots += OrbDot(
                x = px, y = py, z = z,
                r = ((o["rBase"] ?: 0.6) + (o["rDepth"] ?: 1.7) * depth + (o["rBoost"] ?: 1.0) * boost) * rs,
                white = (o["inkFar"] ?: 0.62) - (o["inkSpan"] ?: 0.54) * depth,
                a = dimBase + (1 - dimBase) * min(1.0, boost)
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

internal fun frameRubik(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.82
    val pt = Projector(yaw = t * 0.55, tilt = 0.35 + 0.1 * sin(t * 0.9), cx = cx, cy = cy, scale = r)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)
    val moveCount = (o["moveCount"] ?: 14.0).toInt()
    val moves = makeMoves(moveCount)
    val sc = solveCycle(t, moveCount, 0.42, 1.2)

    val dots = mutableListOf<OrbDot>()
    val latRings = (o["latRings"] ?: 15.0).toInt()
    val lonDensity = o["lonDensity"] ?: 40.0
    for (li in 0..latRings) {
        val lat = -PI / 2 + (li.toDouble() / latRings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val lonCount = max(1, (abs(cosLat) * lonDensity).roundToInt())
        for (lj in 0 until lonCount) {
            val lon = (lj.toDouble() / lonCount) * 2 * PI
            val (x, y, z, inActive) = applyMoves(Triple(cosLat * cos(lon), sinLat, cosLat * sin(lon)), moves, sc)
            val (px, py, zr) = pt(x, y, z)
            val depth = (zr + 1) / 2
            dots += OrbDot(
                x = px, y = py, z = zr,
                r = ((o["rBase"] ?: 0.6) + (o["rDepth"] ?: 1.7) * depth + if (inActive) (o["rActive"] ?: 0.3) else 0.0) * rs,
                white = (o["inkFar"] ?: 0.62) - (o["inkSpan"] ?: 0.54) * depth - if (inActive) 0.14 else 0.0
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

internal fun frameWave(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.874
    val pt = Projector(yaw = t * 0.18, tilt = 0.38, cx = cx, cy = cy, scale = 1.0)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)

    val dots = mutableListOf<OrbDot>()
    val rings = (o["rings"] ?: 15.0).toInt()
    val lonDensity = o["lonDensity"] ?: 40.0
    for (ri in 0..rings) {
        val lat = -PI / 2 + (ri.toDouble() / rings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val w = 0.62 * sin(t * 2.1 - ri * 0.52) + 0.38 * sin(t * 1.27 + ri * 0.83)
        val rr = r * (0.88 + 0.105 * w)
        val lonCount = max(1, (abs(cosLat) * lonDensity).roundToInt())
        for (lj in 0 until lonCount) {
            val lon = (lj.toDouble() / lonCount) * 2 * PI
            val (px, py, z) = pt(cosLat * cos(lon) * rr, sinLat * rr, cosLat * sin(lon) * rr)
            val depth = (z / r + 1) / 2
            val crest = max(0.0, w)
            dots += OrbDot(
                x = px, y = py, z = z,
                r = ((o["rBase"] ?: 0.6) + (o["rDepth"] ?: 1.7) * depth) * (1 + 0.4 * crest) * rs,
                white = 0.66 - 0.56 * depth - 0.1 * crest
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

private fun smoothE(x: Double): Double = x * x * (3 - 2 * x)

private class PolyPath(verts: List<Pair<Double, Double>>) {
    val total: Double
    private val segLengths: List<Double>

    init {
        var sum = 0.0
        val lengths = mutableListOf<Double>()
        for (i in verts.indices) {
            val a = verts[i]
            val b = verts[(i + 1) % verts.size]
            val l = sqrt((b.first - a.first).pow(2) + (b.second - a.second).pow(2))
            lengths += l
            sum += l
        }
        segLengths = lengths
        total = sum
    }

    private val verts: List<Pair<Double, Double>> = verts

    fun point(f: Double): Pair<Double, Double> {
        var target = f * total
        var i = 0
        while (target > segLengths[i] && i < verts.size - 1) {
            target -= segLengths[i]
            i++
        }
        val a = verts[i]
        val b = verts[(i + 1) % verts.size]
        val ff = if (segLengths[i] != 0.0) min(1.0, target / segLengths[i]) else 0.0
        return a.first + (b.first - a.first) * ff to a.second + (b.second - a.second) * ff
    }
}

private sealed class Shape {
    data object Circle : Shape()
    data class Poly(val path: PolyPath) : Shape()

    fun point(f: Double): Pair<Double, Double> = when (this) {
        Circle -> {
            val a = -PI / 2 + f * 2 * PI
            cos(a) * 0.24 to sin(a) * 0.24
        }
        is Poly -> path.point(f)
    }
}

private val TRIANGLE = PolyPath(listOf(0.0 to -0.26, 0.24 to 0.16, -0.24 to 0.16))
private val SQUARE = PolyPath(listOf(0.0 to -0.2, 0.2 to -0.2, 0.2 to 0.2, -0.2 to 0.2, -0.2 to -0.2))
private val CYCLE = listOf(Shape.Circle, Shape.Poly(TRIANGLE), Shape.Poly(SQUARE))

private fun morphN(d: Double): Int = max(6, (34 * d).roundToInt())

internal fun frameMorph(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val kCount = CYCLE.size
    val seg = 1.4 + 0.9
    val tc = t.truncatingRemainder(seg * kCount)
    val k = floor(tc / seg).toInt()
    val local = tc - k * seg
    val m = if (local > 1.4) smoothE((local - 1.4) / 0.9) else 0.0
    val sprd = o["spread"] ?: 1.0

    val pA = CYCLE[k]
    val pB = CYCLE[(k + 1) % kCount]
    val mCount = 160
    val pts = List(mCount) { i ->
        val f = i.toDouble() / mCount
        val a = pA.point(f)
        val b = pB.point(f)
        (a.first + (b.first - a.first) * m) * sprd to (a.second + (b.second - a.second) * m) * sprd
    }
    val lengths = List(mCount) { i ->
        val a = pts[i]
        val b = pts[(i + 1) % mCount]
        sqrt((b.first - a.first).pow(2) + (b.second - a.second).pow(2))
    }
    val total = lengths.sum()

    val n = morphN(o["iconD"] ?: 1.0)
    val re = (o["rDot"] ?: 0.021) * 1.35 * sprd
    val pulse = 1 + 0.02 * sin(local * 3.1)

    val dots = mutableListOf<OrbDot>()
    val c2 = size / 2
    var segIdx = 0
    var acc = 0.0
    for (k2 in 0 until n) {
        val target = (k2.toDouble() / n) * total
        while (acc + lengths[segIdx] < target && segIdx < mCount - 1) {
            acc += lengths[segIdx]
            segIdx++
        }
        val a = pts[segIdx]
        val b = pts[(segIdx + 1) % mCount]
        val f = if (lengths[segIdx] != 0.0) min(1.0, (target - acc) / lengths[segIdx]) else 0.0
        val x = (a.first + (b.first - a.first) * f) * pulse
        val y = (a.second + (b.second - a.second) * f) * pulse
        dots += OrbDot(
            x = c2 + x * size,
            y = c2 + y * size,
            z = 0.0,
            r = max(0.35, re * size),
            white = 0.1
        )
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.25)
}

internal fun frameOrbits(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.82
    val pt = Projector(yaw = t * 0.12, tilt = 0.3, cx = cx, cy = cy, scale = 1.0)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)

    val dots = mutableListOf<OrbDot>()
    val orbitN = (o["orbitN"] ?: 12.0).toInt()
    val ghostN = (o["ghostN"] ?: 40.0).toInt()
    val particles = (o["particles"] ?: 3.0).toInt()

    for (orb in 0 until orbitN) {
        val h1 = hashD(orb.toDouble(), 1.7)
        val h2 = hashD(orb.toDouble(), 5.2)
        val h3 = hashD(orb.toDouble(), 8.9)
        val ro = r * (0.45 + 0.52 * h1)
        val th = h1 * 2 * PI
        val phi = acos(2 * h2 - 1)
        val nx = sin(phi) * cos(th)
        val ny = cos(phi)
        val nz = sin(phi) * sin(th)
        var ux = -ny
        var uy = nx
        val uz = 0.0
        val ul = max(1e-6, sqrt(ux * ux + uy * uy))
        ux /= ul
        uy /= ul
        val vx = ny * uz - nz * uy
        val vy = nz * ux - nx * uz
        val vz = nx * uy - ny * ux
        val speed = (0.25 + 0.55 * h3) * if (h3 > 0.5) 1.0 else -1.0

        for (k in 0 until ghostN) {
            val a = (k.toDouble() / ghostN) * 2 * PI
            val (px, py, z) = pt(
                (ux * cos(a) + vx * sin(a)) * ro,
                (uy * cos(a) + vy * sin(a)) * ro,
                (uz * cos(a) + vz * sin(a)) * ro
            )
            val depth = (z / ro + 1) / 2
            dots += OrbDot(
                x = px, y = py, z = z,
                r = (o["ghostR"] ?: 0.9) * rs,
                white = 0.72,
                a = (o["ghostA"] ?: 0.5) * (0.4 + 0.6 * depth)
            )
        }
        for (m in 0 until particles) {
            val a = t * speed + (m.toDouble() / particles) * 2 * PI + h2 * 6
            val (px, py, z) = pt(
                (ux * cos(a) + vx * sin(a)) * ro,
                (uy * cos(a) + vy * sin(a)) * ro,
                (uz * cos(a) + vz * sin(a)) * ro
            )
            val depth = (z / ro + 1) / 2
            dots += OrbDot(
                x = px, y = py, z = z,
                r = ((o["partR"] ?: 1.2) + (o["partRDepth"] ?: 1.6) * depth) * rs,
                white = 0.3 - 0.22 * depth
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

internal fun frameBraid(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.76
    val pt = Projector(yaw = t * 0.4, tilt = 0.3, cx = cx, cy = cy, scale = 1.0)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)

    val dots = mutableListOf<OrbDot>()
    val ghostN = (o["ghostN"] ?: 150.0).toInt()
    for (i in 0 until ghostN) {
        val d = fibDir(i, ghostN)
        val (px, py, z) = pt(d.first * r, d.second * r, d.third * r)
        val depth = (z / r + 1) / 2
        dots += OrbDot(x = px, y = py, z = z, r = 0.8 * rs, white = 0.78, a = 0.1 + 0.22 * depth)
    }

    val strandN = (o["strandN"] ?: 52.0).toInt()
    val turns = o["turns"] ?: 3.0
    for (s in 0 until 3) {
        val phase = (s.toDouble() / 3) * 2 * PI
        for (i in 0 until strandN) {
            val u = (frac(i.toDouble() / strandN + t * 0.045) * 2 - 1) * 0.96
            val surf = max(0.0, sqrt(1 - u * u))
            val endFade = min(1.0, (1 - abs(u)) / 0.1)
            val a = u * PI * turns + phase
            val weave = 1 + 0.075 * sin(u * PI * turns * 2 + phase * 2 + t * 0.8)
            val rr = surf * r * weave
            val (px, py, zr) = pt(cos(a) * rr, u * r * weave, sin(a) * rr)
            val depth = (zr / r + 1) / 2
            dots += OrbDot(
                x = px, y = py, z = zr,
                r = ((o["rBase"] ?: 1.2) + (o["rDepth"] ?: 1.8) * depth) * rs,
                white = 0.55 - 0.45 * depth,
                a = endFade * (0.45 + 0.55 * depth)
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

internal fun frameRibbon(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.78
    val spin = o["spin"] ?: 1.0
    val camTilt = 0.3
    val faceOn = (o["faceOn"] ?: 0.0) != 0.0
    val pt = Projector(yaw = t * 0.1 * spin, tilt = camTilt, cx = cx, cy = cy, scale = 1.0)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)

    val dots = mutableListOf<OrbDot>()
    val ghostN = (o["ghostN"] ?: 150.0).toInt()
    if (ghostN > 0) {
        for (i in 0 until ghostN) {
            val d = fibDir(i, ghostN)
            val (px, py, z) = pt(d.first * r, d.second * r, d.third * r)
            val depth = (z / r + 1) / 2
            dots += OrbDot(x = px, y = py, z = z, r = 0.8 * rs, white = 0.78, a = 0.1 + 0.22 * depth)
        }
    }

    val ya = t * 0.24 * spin
    val ta = if (faceOn) -camTilt else 0.55 + 0.3 * sin(t * 0.18) * spin
    val ux = cos(ya)
    val uy = 0.0
    val uz = sin(ya)
    val vx = -uz * sin(ta)
    val vy = cos(ta)
    val vz = ux * sin(ta)
    val nx = uy * vz - uz * vy
    val ny = uz * vx - ux * vz
    val nz = ux * vy - uy * vx

    val wobMul = o["wobMul"] ?: 1.0
    val wobAmp = 0.23 * wobMul
    val baseR = if (faceOn) r / (1 + 0.85 * wobAmp) else r

    val baseLanes = o["lanes"] ?: 5.0
    val segs = (o["segs"] ?: 88.0).toInt()
    val lanes = max(1, ((baseLanes * (o["bandMul"] ?: 1.0)).roundToInt()))
    for (w in 0 until lanes) {
        val laneOff = (w.toDouble() - (lanes - 1) / 2.0) * 0.075
        val edge = abs(w.toDouble() - (lanes - 1) / 2.0) / max(1.0, (lanes - 1) / 2.0)
        for (k in 0 until segs) {
            val a = (k.toDouble() / segs) * 2 * PI
            val wob = (0.16 * sin(a * 3 - t * 1.7 + w * 0.22) + 0.07 * sin(a * 5 + t * 1.1)) * wobMul
            val radial = if (faceOn) 1 + wob else 1.0
            val off = if (faceOn) laneOff else laneOff + wob
            var x = ux * cos(a) + vx * sin(a) + nx * off
            var y = uy * cos(a) + vy * sin(a) + ny * off
            var z = uz * cos(a) + vz * sin(a) + nz * off
            val l = sqrt(x * x + y * y + z * z)
            val rr = baseR * radial
            val (px, py, zr) = pt((x / l) * rr, (y / l) * rr, (z / l) * rr)
            val depth = (zr / r + 1) / 2
            dots += OrbDot(
                x = px, y = py, z = zr,
                r = ((o["rBase"] ?: 1.1) + (o["rDepth"] ?: 1.7) * depth) * (1 - 0.25 * edge) * rs,
                white = 0.52 - 0.44 * depth + 0.18 * edge,
                a = 0.4 + 0.6 * depth
            )
        }
    }
    return finalizeFrame(dots, emptyList(), rMin = o["rMin"] ?: 0.3)
}

internal fun frameWeb(size: Double, t: Double, o: Map<String, Double>): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val r = (size / 2) * 0.8 * (o["spread"] ?: 1.0)
    val pt = Projector(yaw = t * 0.12, tilt = 0.32, cx = cx, cy = cy, scale = r)
    val rs = radiusScale(size, o["rsPow"] ?: 0.6)

    val nodeN = (o["nodeN"] ?: 30.0).toInt()
    val thr = o["thr"] ?: 0.72
    val nodeR = o["nodeR"] ?: 1.4
    val nodeRDepth = o["nodeRDepth"] ?: 1.8

    val nodes = List(nodeN) { i ->
        val d = fibDir(i, nodeN)
        var x = d.first + 0.3 * (vnoise(i * 0.31 + 9, t * 0.24) - 0.5) * 2
        var y = d.second + 0.3 * (vnoise(i * 0.53 + 27, t * 0.21) - 0.5) * 2
        var z = d.third + 0.3 * (vnoise(i * 0.77 + 55, t * 0.27) - 0.5) * 2
        val l = sqrt(x * x + y * y + z * z)
        Triple(x / l, y / l, z / l)
    }

    val lines = mutableListOf<OrbLine>()
    val dots = mutableListOf<OrbDot>()

    for (i in 0 until nodeN) {
        for (j in i + 1 until nodeN) {
            val dx = nodes[i].first - nodes[j].first
            val dy = nodes[i].second - nodes[j].second
            val dz = nodes[i].third - nodes[j].third
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist >= thr) continue
            val (x1, y1, z1) = pt(nodes[i].first, nodes[i].second, nodes[i].third)
            val (x2, y2, z2) = pt(nodes[j].first, nodes[j].second, nodes[j].third)
            val depth = ((z1 + z2) / 2 + 1) / 2
            lines += OrbLine(
                x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                white = 0.42,
                a = (1 - dist / thr) * (0.3 + 0.55 * depth),
                w = max(0.6, (o["lineW"] ?: 0.8) * rs)
            )
        }
    }

    for (i in 0 until nodeN) {
        val (px, py, z) = pt(nodes[i].first, nodes[i].second, nodes[i].third)
        val depth = (z + 1) / 2
        val pulse = 1 + 0.25 * sin(t * 1.4 + i * 2.7)
        dots += OrbDot(
            x = px, y = py, z = z,
            r = (nodeR + nodeRDepth * depth) * pulse * rs,
            white = 0.55 - 0.45 * depth
        )
    }

    val signals = (o["signals"] ?: 5.0).toInt()
    for (s in 0 until signals) {
        val seg = floor(t * 0.55 + s * 7.31)
        val a = floor(hashD(seg, s * 3.1 + 1.7) * nodeN).toInt()
        val b = floor(hashD(seg, s * 5.7 + 4.2) * nodeN).toInt()
        if (a == b) continue
        val f = frac(t * 0.55 + s * 7.31)
        var x = lerp(nodes[a].first, nodes[b].first, f)
        var y = lerp(nodes[a].second, nodes[b].second, f)
        var z = lerp(nodes[a].third, nodes[b].third, f)
        val l = max(1e-6, sqrt(x * x + y * y + z * z))
        val (px, py, zr) = pt(x / l, y / l, z / l)
        val depth = (zr + 1) / 2
        dots += OrbDot(
            x = px, y = py, z = zr,
            r = (nodeR * 1.5 + nodeRDepth * depth) * rs,
            white = 0.05,
            a = 0.5 + 0.5 * depth
        )
    }

    return finalizeFrame(dots, lines, rMin = o["rMin"] ?: 0.3)
}
