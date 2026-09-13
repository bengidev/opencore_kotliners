package io.github.bengidev.opencore.chat.utilities

internal sealed interface ChatRichContentSegment {
    data class Prose(val markdown: String) : ChatRichContentSegment

    /** Syntactically incomplete fragment — rendered as markdown while streaming. */
    data class RawFragment(val text: String) : ChatRichContentSegment

    data class MermaidDiagram(val source: String) : ChatRichContentSegment

    data class MathBlock(val latex: String) : ChatRichContentSegment
}
