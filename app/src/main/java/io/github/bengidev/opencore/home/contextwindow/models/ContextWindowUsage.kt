package io.github.bengidev.opencore.home.contextwindow.models

import kotlin.math.abs
import kotlin.math.roundToInt

/** Normalized context window load for the active conversation and model. */
internal class ContextWindowUsage private constructor(
    val tokensUsed: Int,
    val tokenLimit: Int,
) {
    val hasKnownLimit: Boolean get() = tokenLimit > 0

    val tokensRemaining: Int
        get() = if (hasKnownLimit) maxOf(0, tokenLimit - tokensUsed) else 0

    val fractionUsed: Double
        get() = if (hasKnownLimit) minOf(1.0, tokensUsed.toDouble() / tokenLimit) else 0.0

    val percentUsed: Int
        get() = (fractionUsed * 100).roundToInt()

    val percentRemaining: Int
        get() = if (hasKnownLimit) maxOf(0, 100 - percentUsed) else 0

    val tokensUsedFormatted: String
        get() = compactTokenLabel(tokensUsed)

    val tokenLimitFormatted: String
        get() = compactTokenLabel(tokenLimit)

    companion object {
        val zero: ContextWindowUsage = invoke(tokensUsed = 0, tokenLimit = 0)

        operator fun invoke(tokensUsed: Int, tokenLimit: Int): ContextWindowUsage {
            val limit = maxOf(0, tokenLimit)
            val rawUsed = maxOf(0, tokensUsed)
            val used = if (limit > 0) minOf(rawUsed, limit) else rawUsed
            return ContextWindowUsage(tokensUsed = used, tokenLimit = limit)
        }

        private fun compactTokenLabel(tokens: Int): String =
            when {
                tokens >= 1_000_000 -> formatCompact(tokens / 1_000_000.0, "M")
                tokens >= 1_000 -> formatCompact(tokens / 1_000.0, "K")
                else -> tokens.toString()
            }

        private fun formatCompact(value: Double, suffix: String): String {
            val oneDecimal = (value * 10).roundToInt() / 10.0
            return if (abs(oneDecimal - oneDecimal.roundToInt()) < 0.001) {
                "${oneDecimal.toInt()}$suffix"
            } else {
                "%.1f".format(oneDecimal) + suffix
            }
        }
    }
}
