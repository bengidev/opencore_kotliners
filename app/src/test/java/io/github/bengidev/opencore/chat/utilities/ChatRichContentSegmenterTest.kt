package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRichContentSegmenterTest {
    @Test
    fun segment_proseOnly_returnsSingleProseSegment() {
        val markdown = "GeForce is NVIDIA's brand for consumer GPUs."

        val segments = ChatRichContentSegmenter.segment(markdown)

        assertEquals(1, segments.size)
        assertEquals(ChatRichContentSegment.Prose(markdown), segments[0])
    }

    @Test
    fun segment_mermaidFence_extractsDiagramSegment() {
        val markdown =
            """
            Before text

            ```mermaid
            graph TD
                A --> B
            ```

            After text
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown)

        assertEquals(3, segments.size)
        assertEquals(
            ChatRichContentSegment.Prose("Before text\n\n"),
            segments[0],
        )
        assertEquals(
            ChatRichContentSegment.MermaidDiagram(
                """
                graph TD
                    A --> B
                """.trimIndent(),
            ),
            segments[1],
        )
        assertEquals(
            ChatRichContentSegment.Prose("\n\nAfter text"),
            segments[2],
        )
    }

    @Test
    fun segment_latexFence_extractsMathBlock() {
        val markdown =
            """
            Inline intro

            ```latex
            E = mc^2
            ```
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown)

        assertEquals(2, segments.size)
        assertEquals(
            ChatRichContentSegment.Prose("Inline intro\n\n"),
            segments[0],
        )
        assertEquals(
            ChatRichContentSegment.MathBlock("E = mc^2"),
            segments[1],
        )
    }

    @Test
    fun segment_kotlinFence_staysInProse() {
        val fence =
            """
            ```kotlin
            fun main() {}
            ```
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(fence)

        assertEquals(1, segments.size)
        assertTrue(segments[0] is ChatRichContentSegment.Prose)
        assertEquals(fence, (segments[0] as ChatRichContentSegment.Prose).markdown)
    }

    @Test
    fun segment_unclosedFence_treatsAsProse() {
        val markdown = "```open"

        val segments = ChatRichContentSegmenter.segment(markdown)

        assertEquals(1, segments.size)
        assertEquals(ChatRichContentSegment.Prose(markdown), segments[0])
    }

    @Test
    fun segmentProgressive_unclosedFence_emitsRawTail() {
        val markdown = "Intro\n\n```mermaid\ngraph TD"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Intro\n\n"), segments[0])
        assertEquals(
            ChatRichContentSegment.RawFragment("```mermaid\ngraph TD"),
            segments[1],
        )
    }

    @Test
    fun segmentProgressive_closedMermaid_rendersDiagramBeforeTrailingProse() {
        val markdown =
            """
            ```mermaid
            graph TD
              A --> B
            ```

            Still typing
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertTrue(segments[0] is ChatRichContentSegment.MermaidDiagram)
        assertEquals(ChatRichContentSegment.Prose("\n\nStill typing"), segments[1])
    }

    @Test
    fun segmentProgressive_unclosedInlineBacktick_splitsRichPrefixFromRawTail() {
        val markdown = "Done **bold** and `partial"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Done **bold** and "), segments[0])
        assertEquals(ChatRichContentSegment.RawFragment("`partial"), segments[1])
    }

    @Test
    fun segmentProgressive_tailExtractsHeadingsAndListsForRichRendering() {
        val markdown = "Partial `token\n\n## TL;DR\n\n- First point\n- Second point"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("## TL;DR")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("- First point")
            }
        )
        assertFalse(
            segments.any { segment ->
                segment is ChatRichContentSegment.RawFragment && segment.text.contains("## TL;DR")
            }
        )
    }

    @Test
    fun segmentProgressive_tailBatchesConsecutiveListLines() {
        val markdown = "Partial\n\n- First point\n- Second point\n- Third point"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)
        val listSegments = segments.filter {
            it is ChatRichContentSegment.Prose && it.markdown.contains("- First point")
        }

        assertEquals(1, listSegments.size)
        val markdownSegment = listSegments.single() as ChatRichContentSegment.Prose
        assertTrue(markdownSegment.markdown.contains("- Second point"))
        assertTrue(markdownSegment.markdown.contains("- Third point"))
    }

    @Test
    fun segmentProgressive_tailExtractsBlockquotesForRichRendering() {
        val markdown = "Partial\n\n> First quote line\n> Second quote line"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("> First quote line")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("> Second quote line")
            }
        )
        assertFalse(
            segments.any { segment ->
                segment is ChatRichContentSegment.RawFragment && segment.text.contains("> First quote line")
            }
        )
    }

    @Test
    fun segmentProgressive_tailExtractsTablesForRichRendering() {
        val markdown =
            """
            Partial

            | Name | Value |
            | --- | --- |
            | Alpha | 1 |
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("| Name | Value |")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("| Alpha | 1 |")
            }
        )
        assertFalse(
            segments.any { segment ->
                segment is ChatRichContentSegment.RawFragment && segment.text.contains("| Name | Value |")
            }
        )
    }

    @Test
    fun segmentProgressive_completedParagraphs_freezeBeforeGrowingTail() {
        val markdown = "First paragraph.\n\nSecond paragraph.\n\nThird still typing"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("First paragraph.")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("Second paragraph.")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.RawFragment && segment.text.contains("Third still typing")
            }
        )
    }

    @Test
    fun segment_displayMath_extractsMathBlock() {
        val markdown =
            """
            Intro

            $$
            \int_0^1 x^2\,dx = \frac{1}{3}
            $$

            Outro
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown)

        assertEquals(3, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Intro\n\n"), segments[0])
        assertTrue(segments[1] is ChatRichContentSegment.MathBlock)
        assertTrue((segments[1] as ChatRichContentSegment.MathBlock).latex.contains("\\int_0^1"))
        assertEquals(ChatRichContentSegment.Prose("\n\nOutro"), segments[2])
    }

    @Test
    fun segmentProgressive_unclosedInlineDollar_splitsRichPrefixFromRawTail() {
        val markdown = "Partial \$E = mc"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Partial "), segments[0])
        assertEquals(ChatRichContentSegment.RawFragment("\$E = mc"), segments[1])
    }

    @Test
    fun segment_currencyDollar_keepsGfmTableAsProse() {
        val markdown =
            """
            3. Why ReLU Is So Popular

            | Property | What It Means |
            |----------|---------------|
            | **Speed** | Costs less than $5 per layer |
            """.trimIndent()

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertFalse(
            segments.any { segment ->
                segment is ChatRichContentSegment.RawFragment && segment.text.contains("| Property |")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("| Property |")
            }
        )
        assertTrue(
            segments.any { segment ->
                segment is ChatRichContentSegment.Prose && segment.markdown.contains("**Speed**")
            }
        )
    }

    @Test
    fun segmentProgressive_unclosedDisplayMath_splitsRichPrefixFromRawTail() {
        val markdown = "Partial \$\$E = mc"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Partial "), segments[0])
        assertEquals(ChatRichContentSegment.RawFragment("\$\$E = mc"), segments[1])
    }

    @Test
    fun segmentProgressive_unclosedBracketMath_splitsRichPrefixFromRawTail() {
        val markdown = "Partial \\[E = mc"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(2, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Partial "), segments[0])
        assertEquals(ChatRichContentSegment.RawFragment("\\[E = mc"), segments[1])
    }

    @Test
    fun segmentProgressive_completedProseBeforeRawTail_splitsRichAndRaw() {
        val markdown = "Done **bold**\n\n```mermaid\ngraph TD\n```\n\nTail `open"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(4, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Done **bold**\n\n"), segments[0])
        assertTrue(segments[1] is ChatRichContentSegment.MermaidDiagram)
        assertEquals(ChatRichContentSegment.Prose("\n\nTail "), segments[2])
        assertEquals(ChatRichContentSegment.RawFragment("`open"), segments[3])
    }
}
