package io.github.bengidev.opencore.chat.presenter

/** When the chat thread should auto-scroll and whether animation is appropriate. */
internal object ChatThreadScrollPolicy {
    fun shouldAnimateScroll(
        isBulkRestore: Boolean,
        streamingRevision: Int,
        imeVisible: Boolean,
        previousMessageCount: Int,
    ): Boolean =
        !isBulkRestore &&
            streamingRevision == 0 &&
            !imeVisible &&
            previousMessageCount <= 1

    /** Reasoning-card collapse shrinks row height; animated scroll flashes prior turns. */
    fun shouldAnimateReasoningCollapseScroll(): Boolean = false

    fun shouldDeferForActiveScroll(isScrollInProgress: Boolean): Boolean = isScrollInProgress

    fun isBulkRestore(previousMessageCount: Int, messageCount: Int): Boolean =
        previousMessageCount == 0 && messageCount > 1
}
