package io.github.bengidev.opencore.chat.presenter

/** When the chat thread should auto-scroll and whether animation is appropriate. */
internal object ChatThreadScrollPolicy {
    fun shouldAnimateScroll(
        isBulkRestore: Boolean,
        streamingRevision: Int,
        imeVisible: Boolean,
    ): Boolean = !isBulkRestore && streamingRevision == 0 && !imeVisible

    fun shouldDeferForActiveScroll(isScrollInProgress: Boolean): Boolean = isScrollInProgress

    fun isBulkRestore(previousMessageCount: Int, messageCount: Int): Boolean =
        previousMessageCount == 0 && messageCount > 1
}
