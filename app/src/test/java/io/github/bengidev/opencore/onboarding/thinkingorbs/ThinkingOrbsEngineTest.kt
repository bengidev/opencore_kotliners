package io.github.bengidev.opencore.onboarding.thinkingorbs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingOrbsEngineTest {

    @Test
    fun orbFrame_breathing_producesDots() {
        val frame = orbFrame(OrbState.BREATHING, OrbSize.PX64, t = 1.2)
        assertTrue(frame.dots.isNotEmpty())
    }

    @Test
    fun orbFrame_working_producesDots() {
        val frame = orbFrame(OrbState.WORKING, OrbSize.PX64, t = 0.8)
        assertTrue(frame.dots.isNotEmpty())
    }

    @Test
    fun orbFrame_connecting_producesLines() {
        val frame = orbFrame(OrbState.CONNECTING, OrbSize.PX64, t = 2.0)
        assertTrue(frame.lines.isNotEmpty())
        assertTrue(frame.dots.isNotEmpty())
    }

    @Test
    fun orbFrame_isDeterministicForKnownInputs() {
        val breathing = orbFrame(OrbState.BREATHING, OrbSize.PX64, t = 1.2)
        val working = orbFrame(OrbState.WORKING, OrbSize.PX64, t = 0.8)
        val connecting = orbFrame(OrbState.CONNECTING, OrbSize.PX64, t = 2.0)

        assertEquals(frameFingerprint(breathing), frameFingerprint(breathing))
        assertEquals(frameFingerprint(working), frameFingerprint(working))
        assertEquals(frameFingerprint(connecting), frameFingerprint(connecting))

        val breathingFingerprint = frameFingerprint(breathing)
        val workingFingerprint = frameFingerprint(working)
        val connectingFingerprint = frameFingerprint(connecting)

        assertEquals(3_358_548_326_805_899_979L, breathingFingerprint)
        assertEquals(-1_189_196_513_264_998_506L, workingFingerprint)
        assertEquals(connectingFingerprint, frameFingerprint(connecting))
        assertTrue(connectingFingerprint != 0L)
    }

    @Test
    fun resolvePreset_isCachedPerStateAndSize() {
        val first = OrbSpec.resolvePreset(OrbState.SHAPING, OrbSize.PX64)
        val second = OrbSpec.resolvePreset(OrbState.SHAPING, OrbSize.PX64)
        assertEquals(first.mode, second.mode)
        assertEquals(first.speed, second.speed, 0.0001)
    }

    @Test
    fun thinkingOrbStateForIndex_cyclesFourStates() {
        assertEquals(OrbState.BREATHING, thinkingOrbStateForIndex(0))
        assertEquals(OrbState.COMPOSING, thinkingOrbStateForIndex(1))
        assertEquals(OrbState.SHAPING, thinkingOrbStateForIndex(2))
        assertEquals(OrbState.WORKING, thinkingOrbStateForIndex(3))
        assertEquals(OrbState.BREATHING, thinkingOrbStateForIndex(4))
    }
}

private fun frameFingerprint(frame: OrbFrame): Long {
    var hash = 17L
    for (dot in frame.dots) {
        hash = 31 * hash + dot.x.toRawBits()
        hash = 31 * hash + dot.y.toRawBits()
        hash = 31 * hash + dot.r.toRawBits()
    }
    for (line in frame.lines) {
        hash = 31 * hash + line.x1.toRawBits()
        hash = 31 * hash + line.y1.toRawBits()
        hash = 31 * hash + line.x2.toRawBits()
        hash = 31 * hash + line.y2.toRawBits()
    }
    return hash
}
