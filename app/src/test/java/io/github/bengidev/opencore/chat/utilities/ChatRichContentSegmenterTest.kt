package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertEquals
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
    fun segmentProgressive_unclosedInlineBacktick_emitsRawProse() {
        val markdown = "Done **bold** and `partial"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(1, segments.size)
        assertEquals(ChatRichContentSegment.RawFragment(markdown), segments[0])
    }

    @Test
    fun segmentProgressive_completedProseBeforeRawTail_splitsRichAndRaw() {
        val markdown = "Done **bold**\n\n```mermaid\ngraph TD\n```\n\nTail `open"

        val segments = ChatRichContentSegmenter.segment(markdown, progressive = true)

        assertEquals(3, segments.size)
        assertEquals(ChatRichContentSegment.Prose("Done **bold**\n\n"), segments[0])
        assertTrue(segments[1] is ChatRichContentSegment.MermaidDiagram)
        assertEquals(ChatRichContentSegment.RawFragment("\n\nTail `open"), segments[2])
    }
}
