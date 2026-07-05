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
}
