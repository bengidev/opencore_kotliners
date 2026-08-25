package io.github.bengidev.opencore.onboarding.presenter.cube

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

internal data class CubeVertex(val x: Float, val y: Float, val z: Float)

internal data class CubeEdge(val start: Int, val end: Int)

internal data class CubeFrame(
    val projectedVertices: List<Offset>,
    val edges: List<CubeEdge>,
    val dashedEdgeIndices: Set<Int>,
    val vertexOpacities: List<Float>,
    val edgeStrokeEnds: List<Float>
)

internal class CubeRenderer(
    private val random: Random = Random.Default
) {
    private val vertices = listOf(
        CubeVertex(-1f, -1f, -1f), CubeVertex(1f, -1f, -1f),
        CubeVertex(1f, 1f, -1f), CubeVertex(-1f, 1f, -1f),
        CubeVertex(-1f, -1f, 1f), CubeVertex(1f, -1f, 1f),
        CubeVertex(1f, 1f, 1f), CubeVertex(-1f, 1f, 1f)
    )

    private val edges = listOf(
        CubeEdge(0, 1), CubeEdge(1, 2), CubeEdge(2, 3), CubeEdge(3, 0),
        CubeEdge(4, 5), CubeEdge(5, 6), CubeEdge(6, 7), CubeEdge(7, 4),
        CubeEdge(0, 4), CubeEdge(1, 5), CubeEdge(2, 6), CubeEdge(3, 7)
    )

    private val dashedEdges = setOf(6, 7, 11)

    private enum class Phase { Construction, Morph }

    private var phase = Phase.Construction
    private var isAppeared = false
    private var isMorphPaused = false
    private var reduceMotion = false
    private var rotationProgress = 0f
    private var rotationFromYaw: Float? = null
    private var rotationFromPitch: Float? = null
    private var rotationFromRoll: Float? = null

    private val baseYaw = 0.6f
    private val basePitch = 0.52f
    private val baseRoll = 0f

    private var morphFromYaw = baseYaw
    private var morphFromPitch = basePitch
    private var morphFromRoll = baseRoll
    private var morphToYaw = baseYaw
    private var morphToPitch = basePitch
    private var morphToRoll = baseRoll
    private var morphSegmentStartElapsed: Double? = null
    private var morphSegmentDuration = 0.52
    private var lastElapsedSeconds: Double? = null

    private val headerIconYaw = (Math.PI / 4).toFloat()
    private val headerIconPitch = kotlin.math.atan(1.0 / kotlin.math.sqrt(2.0)).toFloat()
    private val headerIconRoll = 0f

    private val constructionDuration = 0.75
    private val vertexPhaseEnd = 0.35f
    private val edgePhaseStart = 0.2f
    private val morphOverlapStart = 0.72f
    private val morphSegmentOverlap = 0.88f

    fun setAppeared(value: Boolean) {
        if (isAppeared == value) return
        isAppeared = value
        if (value) {
            resetMorphState()
        } else {
            resetMorphState()
        }
    }

    fun setReduceMotion(value: Boolean) {
        if (reduceMotion == value) return
        reduceMotion = value
        if (value) {
            resetMorphState()
        }
    }

    fun setMorphPaused(value: Boolean) {
        isMorphPaused = value
    }

    fun setRotationProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (rotationProgress == clamped) return

        if (rotationProgress == 0f && clamped > 0f) {
            val orientation = currentFreeOrientation(lastElapsedSeconds)
            rotationFromYaw = orientation.first
            rotationFromPitch = orientation.second
            rotationFromRoll = orientation.third
            phase = Phase.Morph
        }

        rotationProgress = clamped

        if (clamped >= 1f) {
            morphFromYaw = headerIconYaw
            morphFromPitch = headerIconPitch
            morphFromRoll = headerIconRoll
            morphToYaw = headerIconYaw
            morphToPitch = headerIconPitch
            morphToRoll = headerIconRoll
            morphSegmentStartElapsed = null
        }
    }

    fun tick(elapsedSeconds: Double, width: Float, height: Float): CubeFrame {
        lastElapsedSeconds = elapsedSeconds
        if (!isAppeared) {
            return buildFrame(0f, null, width, height)
        }

        if (reduceMotion) {
            return buildFrame(1f, null, width, height)
        }

        if (isMorphPaused) {
            return buildFrame(1f, elapsedSeconds, width, height)
        }

        if (rotationProgress > 0f) {
            return buildFrame(1f, elapsedSeconds, width, height)
        }

        val progress = (elapsedSeconds / constructionDuration).toFloat().coerceIn(0f, 1f)

        if (progress >= morphOverlapStart && phase == Phase.Construction) {
            phase = Phase.Morph
            beginMorphSegment(elapsedSeconds, immediate = true)
        } else if (phase == Phase.Morph) {
            advanceMorphIfNeeded(elapsedSeconds)
        }

        return buildFrame(progress, elapsedSeconds, width, height)
    }

    private fun resetMorphState() {
        phase = Phase.Construction
        morphFromYaw = baseYaw
        morphFromPitch = basePitch
        morphFromRoll = baseRoll
        morphToYaw = baseYaw
        morphToPitch = basePitch
        morphToRoll = baseRoll
        morphSegmentStartElapsed = null
        morphSegmentDuration = 0.52
        rotationProgress = 0f
        rotationFromYaw = null
        rotationFromPitch = null
        rotationFromRoll = null
        lastElapsedSeconds = null
    }

    private fun beginMorphSegment(elapsedSeconds: Double, immediate: Boolean = false) {
        val (yaw, pitch, roll) = currentOrientation(elapsedSeconds)
        morphFromYaw = yaw
        morphFromPitch = pitch
        morphFromRoll = roll
        val target = randomMorphTarget(Triple(yaw, pitch, roll), preferNoticeableChange = immediate)
        morphToYaw = target.first
        morphToPitch = target.second
        morphToRoll = target.third
        morphSegmentDuration = if (immediate) {
            random.nextDouble(0.34, 0.46)
        } else {
            random.nextDouble(0.38, 0.52)
        }
        morphSegmentStartElapsed = elapsedSeconds
    }

    private fun advanceMorphIfNeeded(elapsedSeconds: Double) {
        val segmentStart = morphSegmentStartElapsed
        if (segmentStart == null) {
            beginMorphSegment(elapsedSeconds)
            return
        }
        val elapsed = elapsedSeconds - segmentStart
        if (elapsed >= morphSegmentDuration * morphSegmentOverlap) {
            beginMorphSegment(elapsedSeconds)
        }
    }

    private fun randomMorphTarget(
        current: Triple<Float, Float, Float>?,
        preferNoticeableChange: Boolean
    ): Triple<Float, Float, Float> {
        val minimumDelta = if (preferNoticeableChange) 0.1f else 0.045f
        repeat(8) {
            val candidate = Triple(
                random.nextFloat() * 0.7f + 0.35f,
                random.nextFloat() * 0.34f + 0.28f,
                random.nextFloat() * 0.32f - 0.16f
            )
            if (current == null) return candidate
            val delta = kotlin.math.abs(candidate.first - current.first) +
                kotlin.math.abs(candidate.second - current.second) +
                kotlin.math.abs(candidate.third - current.third)
            if (delta >= minimumDelta) return candidate
        }
        return Triple(
            (current?.first ?: baseYaw) + 0.22f,
            (current?.second ?: basePitch) + 0.12f,
            (current?.third ?: baseRoll) + 0.08f
        )
    }

    private fun easeOut(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return 1f - (1f - clamped).pow(3)
    }

    private fun easeInOut(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return if (clamped < 0.5f) {
            4f * clamped * clamped * clamped
        } else {
            1f - (-2f * clamped + 2f).pow(3) / 2f
        }
    }

    private fun currentFreeOrientation(elapsedSeconds: Double?): Triple<Float, Float, Float> {
        if (phase != Phase.Morph || elapsedSeconds == null) {
            return Triple(baseYaw, basePitch, baseRoll)
        }
        val segmentStart = morphSegmentStartElapsed ?: return Triple(baseYaw, basePitch, baseRoll)
        val t = easeInOut(((elapsedSeconds - segmentStart) / morphSegmentDuration).toFloat())
        return Triple(
            morphFromYaw + (morphToYaw - morphFromYaw) * t,
            morphFromPitch + (morphToPitch - morphFromPitch) * t,
            morphFromRoll + (morphToRoll - morphFromRoll) * t
        )
    }

    private fun currentOrientation(elapsedSeconds: Double?): Triple<Float, Float, Float> {
        if (rotationProgress > 0f) {
            val fromYaw = rotationFromYaw ?: morphFromYaw
            val fromPitch = rotationFromPitch ?: morphFromPitch
            val fromRoll = rotationFromRoll ?: morphFromRoll
            val t = rotationProgress
            return Triple(
                fromYaw + (headerIconYaw - fromYaw) * t,
                fromPitch + (headerIconPitch - fromPitch) * t,
                fromRoll + (headerIconRoll - fromRoll) * t
            )
        }
        return currentFreeOrientation(elapsedSeconds)
    }

    private fun project(
        vertex: CubeVertex,
        center: Offset,
        half: Float,
        yaw: Float,
        pitch: Float,
        roll: Float
    ): Offset {
        val x1 = vertex.x * cos(yaw) + vertex.z * sin(yaw)
        val z1 = -vertex.x * sin(yaw) + vertex.z * cos(yaw)
        val y2 = vertex.y * cos(pitch) - z1 * sin(pitch)
        val x3 = x1 * cos(roll) - y2 * sin(roll)
        val y3 = x1 * sin(roll) + y2 * cos(roll)
        return Offset(center.x + x3 * half, center.y + y3 * half)
    }

    private fun constructionVertexOpacity(index: Int, progress: Float): Float {
        val vertexProgress = (progress / vertexPhaseEnd).coerceAtMost(1f)
        val delay = index * 0.025f
        val normalizedDelay = delay / vertexPhaseEnd
        val linear = ((vertexProgress - normalizedDelay) / (1f - normalizedDelay)).coerceIn(0f, 1f)
        return easeOut(linear)
    }

    private fun constructionEdgeStrokeEnd(index: Int, progress: Float): Float {
        val edgeSpan = (1f - edgePhaseStart).coerceAtLeast(0.001f)
        val edgeProgress = ((progress - edgePhaseStart) / edgeSpan).coerceIn(0f, 1f)
        val linear = if (dashedEdges.contains(index)) {
            ((edgeProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
        } else {
            val delay = index * 0.05f
            ((edgeProgress - delay) / (1f - delay)).coerceIn(0f, 1f)
        }
        return easeOut(linear)
    }

    private fun buildFrame(progress: Float, elapsedSeconds: Double?, width: Float, height: Float): CubeFrame {
        val center = Offset(width / 2f, height / 2f)
        val half = minOf(width, height) * 0.34f
        val orientation = currentOrientation(elapsedSeconds)
        val projected = vertices.map { vertex ->
            project(vertex, center, half, orientation.first, orientation.second, orientation.third)
        }

        val constructionProgress = if (phase == Phase.Morph && progress >= 1f) null else progress

        val vertexOpacities = projected.indices.map { index ->
            if (constructionProgress != null) {
                constructionVertexOpacity(index, constructionProgress)
            } else {
                1f
            }
        }

        val edgeStrokeEnds = edges.indices.map { index ->
            if (constructionProgress != null) {
                constructionEdgeStrokeEnd(index, constructionProgress)
            } else {
                1f
            }
        }

        return CubeFrame(
            projectedVertices = projected,
            edges = edges,
            dashedEdgeIndices = dashedEdges,
            vertexOpacities = vertexOpacities,
            edgeStrokeEnds = edgeStrokeEnds
        )
    }
}
