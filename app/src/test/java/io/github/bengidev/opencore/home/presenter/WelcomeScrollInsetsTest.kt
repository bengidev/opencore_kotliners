package io.github.bengidev.opencore.home.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WelcomeScrollInsetsTest {

    @Test
    fun welcomeImeScrollTarget_waitsForScrollableContent() {
        assertNull(
            welcomeImeScrollTarget(
                imeBottomPx = 400,
                peakImeBottomPx = 400,
                maxScroll = 0,
                currentScroll = 0
            )
        )
    }

    @Test
    fun welcomeImeScrollTarget_mapsInsetProportionally() {
        assertEquals(
            100,
            welcomeImeScrollTarget(
                imeBottomPx = 250,
                peakImeBottomPx = 500,
                maxScroll = 200,
                currentScroll = 0
            )
        )
    }

    @Test
    fun welcomeImeScrollTarget_scrollsToTopWhenKeyboardHidden() {
        assertEquals(
            0,
            welcomeImeScrollTarget(
                imeBottomPx = 0,
                peakImeBottomPx = 0,
                maxScroll = 200,
                currentScroll = 120
            )
        )
    }

    @Test
    fun welcomeImeScrollTarget_skipsScrollWhenAlreadyAtTop() {
        assertNull(
            welcomeImeScrollTarget(
                imeBottomPx = 0,
                peakImeBottomPx = 0,
                maxScroll = 200,
                currentScroll = 0
            )
        )
    }
}
