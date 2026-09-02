package io.github.bengidev.opencore.onboarding.thinkingorbs

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class OrbState(val label: String) {
    WORKING("Working…"),
    SEARCHING("Searching…"),
    SOLVING("Solving…"),
    LISTENING("Listening…"),
    CONNECTING("Connecting…"),
    WEAVING("Weaving…"),
    COMPOSING("Composing…"),
    BREATHING("Thinking…"),
    SHAPING("Shaping…");

    val mode: OrbMode
        get() = when (this) {
            WORKING -> OrbMode.ORBITS
            SEARCHING -> OrbMode.GLOBE
            SOLVING -> OrbMode.RUBIK
            LISTENING -> OrbMode.WAVE
            CONNECTING -> OrbMode.WEB
            WEAVING -> OrbMode.BRAID
            COMPOSING -> OrbMode.RIBBON
            BREATHING -> OrbMode.RING
            SHAPING -> OrbMode.MORPH
        }
}

internal enum class OrbSize(val value: Double) {
    PX64(64.0),
    PX20(20.0)
}

internal enum class OrbMode {
    ORBITS,
    GLOBE,
    RUBIK,
    WAVE,
    WEB,
    BRAID,
    RIBBON,
    RING,
    MORPH
}

internal data class ResolvedPreset(
    val mode: OrbMode,
    val speed: Double,
    val opts: Map<String, Double>
)

internal object OrbSpec {
    const val REDUCED_MOTION_T: Double = 0.6

    private data class Preset(
        val speed: Double,
        val count: Double,
        val size: Double,
        val extra: Map<String, Double> = emptyMap()
    )

    val baseProfiles: Map<OrbMode, Map<String, Double>> = mapOf(
        OrbMode.ORBITS to mapOf(
            "orbitN" to 12.0, "ghostN" to 40.0, "ghostR" to 0.9, "ghostA" to 0.5,
            "particles" to 3.0, "partR" to 1.2, "partRDepth" to 1.6, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.GLOBE to mapOf(
            "latRings" to 17.0, "lonDensity" to 44.0, "rBase" to 0.6, "rDepth" to 1.7,
            "rBoost" to 1.0, "inkFar" to 0.62, "inkSpan" to 0.54, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.RUBIK to mapOf(
            "latRings" to 15.0, "lonDensity" to 40.0, "moveCount" to 14.0, "rBase" to 0.6,
            "rDepth" to 1.7, "rActive" to 0.3, "inkFar" to 0.62, "inkSpan" to 0.54, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.WAVE to mapOf(
            "rings" to 15.0, "lonDensity" to 40.0, "rBase" to 0.6, "rDepth" to 1.7, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.WEB to mapOf(
            "nodeN" to 30.0, "thr" to 0.72, "signals" to 5.0, "nodeR" to 1.4,
            "nodeRDepth" to 1.8, "lineW" to 0.8, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.BRAID to mapOf(
            "strandN" to 52.0, "turns" to 3.0, "ghostN" to 150.0, "rBase" to 1.2,
            "rDepth" to 1.8, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.RIBBON to mapOf(
            "lanes" to 5.0, "segs" to 88.0, "ghostN" to 150.0, "rBase" to 1.1,
            "rDepth" to 1.7, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.RING to mapOf(
            "lanes" to 5.0, "segs" to 88.0, "ghostN" to 0.0, "faceOn" to 1.0,
            "rBase" to 1.1, "rDepth" to 1.7, "rsPow" to 0.6, "rMin" to 0.3
        ),
        OrbMode.MORPH to mapOf("rDot" to 0.021, "iconD" to 1.0, "rMin" to 0.25)
    )

    private val presets: Map<OrbMode, Map<OrbSize, Preset>> = mapOf(
        OrbMode.ORBITS to mapOf(
            OrbSize.PX64 to Preset(1.885, 1.0, 1.0),
            OrbSize.PX20 to Preset(3.9, 0.238, 2.4)
        ),
        OrbMode.GLOBE to mapOf(
            OrbSize.PX64 to Preset(2.015, 0.42, 1.15, mapOf("scanMul" to 4.08, "dimBase" to 0.45)),
            OrbSize.PX20 to Preset(2.665, 0.105, 1.75, mapOf("scanMul" to 4.335, "dimBase" to 0.45))
        ),
        OrbMode.RUBIK to mapOf(
            OrbSize.PX64 to Preset(1.82, 0.35, 1.05),
            OrbSize.PX20 to Preset(1.95, 0.088, 1.9)
        ),
        OrbMode.WAVE to mapOf(
            OrbSize.PX64 to Preset(4.388, 0.341, 1.0),
            OrbSize.PX20 to Preset(3.998, 0.105, 1.6)
        ),
        OrbMode.WEB to mapOf(
            OrbSize.PX64 to Preset(3.315, 1.35, 0.95),
            OrbSize.PX20 to Preset(6.63, 0.25, 1.52)
        ),
        OrbMode.BRAID to mapOf(
            OrbSize.PX64 to Preset(1.625, 0.5, 1.0),
            OrbSize.PX20 to Preset(2.75, 0.1125, 1.36)
        ),
        OrbMode.RIBBON to mapOf(
            OrbSize.PX64 to Preset(2.34, 0.25, 0.85, mapOf("spin" to 0.0, "bandMul" to 3.9, "wobMul" to 1.0)),
            OrbSize.PX20 to Preset(3.12, 0.051, 1.073, mapOf("spin" to 0.0, "bandMul" to 4.94, "wobMul" to 1.0))
        ),
        OrbMode.RING to mapOf(
            OrbSize.PX64 to Preset(3.24, 0.25, 0.956, mapOf("spin" to 0.0, "bandMul" to 3.627, "wobMul" to 0.368)),
            OrbSize.PX20 to Preset(3.78, 0.028, 1.622, mapOf("spin" to 0.0, "bandMul" to 3.968, "wobMul" to 0.565))
        ),
        OrbMode.MORPH to mapOf(
            OrbSize.PX64 to Preset(2.405, 0.702, 0.395, mapOf("spread" to 1.45)),
            OrbSize.PX20 to Preset(2.08, 0.53, 1.011, mapOf("spread" to 1.45))
        )
    )

    private val countPairs = listOf(
        "latRings" to "lonDensity",
        "rings" to "lonDensity",
        "lanes" to "segs"
    )
    private val countKeys = listOf("orbitN", "ghostN", "nodeN", "strandN", "signals")
    private val iconDensityKeys = listOf("iconD")
    private val radiusKeys = listOf(
        "rBase", "rDepth", "rActive", "rDot", "ghostR", "partR", "partRDepth", "nodeR", "nodeRDepth"
    )

    private val cache = mutableMapOf<String, ResolvedPreset>()

    fun resolvePreset(state: OrbState, size: OrbSize): ResolvedPreset {
        val key = "${state.name}-${size.name}"
        cache[key]?.let { return it }

        val mode = state.mode
        val preset = presets.getValue(mode).getValue(size)
        val opts = baseProfiles.getValue(mode).toMutableMap()
        if (preset.count != 1.0) scaleCounts(opts, preset.count)
        if (preset.size != 1.0) scaleRadii(opts, preset.size)
        opts.putAll(preset.extra)

        return ResolvedPreset(mode, preset.speed, opts).also { cache[key] = it }
    }

    private fun scaleCounts(opts: MutableMap<String, Double>, scale: Double) {
        val done = mutableSetOf<String>()
        val rt = sqrt(scale)
        for ((a, b) in countPairs) {
            val va = opts[a]
            val vb = opts[b]
            if (va != null && vb != null && a !in done && b !in done) {
                opts[a] = max(2.0, (va * rt).roundToInt().toDouble())
                opts[b] = max(2.0, (vb * rt).roundToInt().toDouble())
                done += a
                done += b
            }
        }
        for (k in countKeys) {
            val v = opts[k] ?: continue
            if (v != 0.0 && k !in done) {
                opts[k] = max(1.0, (v * scale).roundToInt().toDouble())
            }
        }
        for (k in iconDensityKeys) {
            opts[k]?.let { opts[k] = max(0.02, it * scale) }
        }
    }

    private fun scaleRadii(opts: MutableMap<String, Double>, scale: Double) {
        for (k in radiusKeys) {
            opts[k]?.let { opts[k] = it * scale }
        }
        opts["rSizeMul"] = (opts["rSizeMul"] ?: 1.0) * scale
    }
}

internal fun orbFrame(preset: ResolvedPreset, size: Double, t: Double): OrbFrame {
    val o = preset.opts
    return when (preset.mode) {
        OrbMode.ORBITS -> frameOrbits(size, t, o)
        OrbMode.GLOBE -> frameGlobe(size, t, o)
        OrbMode.RUBIK -> frameRubik(size, t, o)
        OrbMode.WAVE -> frameWave(size, t, o)
        OrbMode.WEB -> frameWeb(size, t, o)
        OrbMode.BRAID -> frameBraid(size, t, o)
        OrbMode.RIBBON, OrbMode.RING -> frameRibbon(size, t, o)
        OrbMode.MORPH -> frameMorph(size, t, o)
    }
}

internal fun orbFrame(state: OrbState, size: OrbSize, t: Double): OrbFrame {
    val preset = OrbSpec.resolvePreset(state, size)
    return orbFrame(preset, size.value, t)
}
