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
            !imeVisible &&
            streamingRevision == 0 &&
            previousMessageCount <= 1

    /** Reasoning-card collapse shrinks row height; animated scroll flashes prior turns. */
    fun shouldAnimateReasoningCollapseScroll(): Boolean = false

    fun shouldDeferForActiveScroll(isScrollInProgress: Boolean): Boolean = isScrollInProgress

    fun isBulkRestore(previousMessageCount: Int, messageCount: Int): Boolean =
        previousMessageCount == 0 && messageCount > 1

    /** Skip redundant scrolls when coalesced flushes did not grow tail content. */
    fun shouldScrollForStreamingUpdate(
        pendingByteCount: Int,
        lastScrolledByteCount: Int,
    ): Boolean = pendingByteCount > lastScrolledByteCount

    fun shouldScrollForNewMessage(
        previousMessageCount: Int,
        messageCount: Int,
    ): Boolean = messageCount > previousMessageCount

    fun shouldScrollForImeChange(
        imeBottomPx: Int,
        previousImeBottomPx: Int,
    ): Boolean = imeBottomPx != previousImeBottomPx

    fun shouldDelayForImeLayout(imeBottomPx: Int): Boolean = imeBottomPx > 0

    /** Rich markdown (tables, embeds) often grows after the stream ends. */
    fun shouldScrollForStreamFinished(
        wasSending: Boolean,
        isSending: Boolean,
    ): Boolean = wasSending && !isSending
}
