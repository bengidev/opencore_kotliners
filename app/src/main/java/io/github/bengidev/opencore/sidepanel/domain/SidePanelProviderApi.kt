package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelProviderApi(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultHeaders: Map<String, String> = emptyMap()
) {
    companion object {
        val openRouter = SidePanelProviderApi(
            id = "openrouter",
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/bengidev/opencore_kotliners",
                "X-Title" to "OpenCore"
            )
        )

        val openCode = SidePanelProviderApi(
            id = "opencode",
            displayName = "OpenCode",
            baseUrl = "https://opencode.ai/zen/v1",
            defaultHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/bengidev/opencore_kotliners",
                "X-Title" to "OpenCore"
            )
        )

        val commandCode = SidePanelProviderApi(
            id = "commandcode",
            displayName = "Command Code",
            baseUrl = "https://api.commandcode.ai/provider/v1"
        )

        val all: List<SidePanelProviderApi> = listOf(openRouter, openCode, commandCode)
        val default: SidePanelProviderApi = openRouter

        fun resolve(id: String?): SidePanelProviderApi =
            id?.let { providerId -> all.firstOrNull { it.id == providerId } } ?: default
    }

    val chatCompletionsUrl: String
        get() = "$baseUrl/chat/completions"

    val modelsUrl: String
        get() = "$baseUrl/models"
}
