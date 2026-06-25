package io.github.bengidev.opencore.chat.presenter

/** Resolves whether the active thread title should render above the message list. */
internal object ChatViewTitlePolicy {
    fun resolve(headerTitle: String): String? = headerTitle.takeIf { it.isNotBlank() }

    fun requiresHeadingSemantics(title: String?): Boolean = !title.isNullOrBlank()
}
