package io.github.bengidev.opencore.chat.presenter

/**
 * Coalesces rapid [TextView] updates onto the UI thread (33ms default).
 * Shared by plain and markdown streaming coordinators.
 */
internal class CoalescedTextViewScheduler(
    private val coalesceDelayMs: Long = COALESCE_DELAY_MS,
) {
    private var boundTextView: ChatStreamingSizingTextView? = null
    private var pendingFlush: ((ChatStreamingSizingTextView) -> Unit)? = null
    private var updateRunnable: Runnable? = null

    fun schedule(
        textView: ChatStreamingSizingTextView,
        onBindingChanged: () -> Unit,
        onFlush: (ChatStreamingSizingTextView) -> Unit,
    ) {
        if (boundTextView !== textView) {
            boundTextView?.let(::cancel)
            boundTextView = textView
            onBindingChanged()
        }
        pendingFlush = onFlush
        if (updateRunnable != null) return

        val runnable = Runnable {
            updateRunnable = null
            val bound = boundTextView ?: return@Runnable
            if (bound.isAttachedToWindow) {
                pendingFlush?.invoke(bound)
            }
        }
        updateRunnable = runnable
        textView.postDelayed(runnable, coalesceDelayMs)
    }

    fun cancel(textView: ChatStreamingSizingTextView) {
        updateRunnable?.let(textView::removeCallbacks)
        updateRunnable = null
        pendingFlush = null
        if (boundTextView === textView) {
            boundTextView = null
        }
    }

    private companion object {
        const val COALESCE_DELAY_MS = 33L
    }
}
