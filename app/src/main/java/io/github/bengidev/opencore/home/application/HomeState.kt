package io.github.bengidev.opencore.home.application

internal data class HomeState(
    val draftMessage: String = "",
    val selectedModelTitle: String = "Free Models Router",
    val contextUsagePercent: Int = 41
) {
    val canSend: Boolean
        get() = draftMessage.isNotBlank()
}
