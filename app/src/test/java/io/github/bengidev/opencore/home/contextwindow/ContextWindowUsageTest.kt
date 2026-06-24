package io.github.bengidev.opencore.home.contextwindow

import io.github.bengidev.opencore.home.contextwindow.models.ContextWindowUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowUsageTest {

    @Test
    fun zeroSentinel_representsUnknownEmptyUsage() {
        val usage = ContextWindowUsage.zero

        assertEquals(0, usage.tokensUsed)
        assertEquals(0, usage.tokenLimit)
        assertEquals(0.0, usage.fractionUsed, 0.0)
        assertEquals(0, usage.percentUsed)
        assertEquals(0, usage.percentRemaining)
    }

    @Test
    fun clampsTokensUsedToLimit() {
        val usage = ContextWindowUsage(tokensUsed = 300_000, tokenLimit = 258_400)

        assertEquals(258_400, usage.tokensUsed)
        assertEquals(1.0, usage.fractionUsed, 0.0)
        assertEquals(100, usage.percentUsed)
        assertEquals(0, usage.percentRemaining)
    }

    @Test
    fun fractionAndPercentCalculations() {
        val usage = ContextWindowUsage(tokensUsed = 129_000, tokenLimit = 258_000)

        assertEquals(0.5, usage.fractionUsed, 0.0)
        assertEquals(50, usage.percentUsed)
        assertEquals(50, usage.percentRemaining)
    }

    @Test
    fun tokenLabelFormatting_compactsThousandsWithOneDecimalWhenNeeded() {
        val usage = ContextWindowUsage(tokensUsed = 158_158, tokenLimit = 258_400)

        assertEquals("158.2K", usage.tokensUsedFormatted)
        assertEquals("258.4K", usage.tokenLimitFormatted)
    }

    @Test
    fun tokenLabelFormatting_omitsDecimalForExactThousands() {
        val usage = ContextWindowUsage(tokensUsed = 107_000, tokenLimit = 258_000)

        assertEquals("107K", usage.tokensUsedFormatted)
        assertEquals("258K", usage.tokenLimitFormatted)
    }

    @Test
    fun tokenLabelFormatting_passesThroughCountsBelowOneThousand() {
        val usage = ContextWindowUsage(tokensUsed = 512, tokenLimit = 999)

        assertEquals("512", usage.tokensUsedFormatted)
        assertEquals("999", usage.tokenLimitFormatted)
    }

    @Test
    fun unknownLimit_reportsZeroFraction() {
        val usage = ContextWindowUsage(tokensUsed = 50_000, tokenLimit = 0)

        assertEquals(0.0, usage.fractionUsed, 0.0)
        assertEquals(0, usage.percentUsed)
        assertEquals(0, usage.percentRemaining)
    }
}
