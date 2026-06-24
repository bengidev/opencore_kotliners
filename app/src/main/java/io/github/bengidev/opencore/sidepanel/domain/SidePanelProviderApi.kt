package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelProviderApi(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val credentialPlaceholder: String,
    val credentialLabel: String,
    val credentialPrompt: String
) {
    companion object {
        val openRouter = SidePanelProviderApi(
            id = "openrouter",
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/bengidev/opencore_kotliners",
                "X-Title" to "OpenCore"
            ),
            credentialPlaceholder = "sk-or-v1-...",
            credentialLabel = "OPENROUTER_API_KEY",
            credentialPrompt = "Create a key at openrouter.ai/keys and paste it here. Requests send Authorization: Bearer <OPENROUTER_API_KEY> per the OpenRouter quickstart. Stored securely on this device."
        )

        val openCode = SidePanelProviderApi(
            id = "opencode",
            displayName = "OpenCode",
            baseUrl = "https://opencode.ai/zen/v1",
            defaultHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/bengidev/opencore_kotliners",
                "X-Title" to "OpenCore"
            ),
            credentialPlaceholder = "API key from opencode.ai/auth",
            credentialLabel = "OpenCode Zen API key",
            credentialPrompt = "Sign in at opencode.ai/auth, add billing, and click Create API Key for OpenCode Zen (opencode.ai/docs/zen). Sent as Authorization: Bearer …. Stored securely on this device."
        )

        val commandCode = SidePanelProviderApi(
            id = "commandcode",
            displayName = "Command Code",
            baseUrl = "https://api.commandcode.ai/provider/v1",
            credentialPlaceholder = "<CMD_API_KEY>",
            credentialLabel = "COMMAND_CODE_API_KEY",
            credentialPrompt = "Generate a key in Command Code Studio (commandcode.ai/docs/studio/api-keys). Same key as COMMAND_CODE_API_KEY — sent as Authorization: Bearer <CMD_API_KEY> per the Provider API docs. Stored securely on this device."
        )

        val ollamaCloud = SidePanelProviderApi(
            id = "ollama",
            displayName = "Ollama Cloud",
            baseUrl = "https://ollama.com/v1",
            credentialPlaceholder = "ollama-...",
            credentialLabel = "OLLAMA_API_KEY",
            credentialPrompt = "Create an API key at ollama.com and paste it here. Sent as Authorization: Bearer \$OLLAMA_API_KEY per docs.ollama.com/api/authentication. Stored securely on this device."
        )

        val all: List<SidePanelProviderApi> = listOf(openRouter, openCode, commandCode, ollamaCloud)
        val default: SidePanelProviderApi = openRouter

        fun resolve(id: String?): SidePanelProviderApi =
            id?.let { providerId -> all.firstOrNull { it.id == providerId } } ?: default
    }

    val chatCompletionsUrl: String
        get() = "$baseUrl/chat/completions"

    val modelsUrl: String
        get() = "$baseUrl/models"
}
