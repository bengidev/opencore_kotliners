package io.github.bengidev.opencore.sidepanel.domain

/** ponytail: static fallback until cache lands; live catalog fills isFree/contextLength. */
internal object SidePanelModelCatalog {
    private val openRouterModels = listOf(
        SidePanelModel(
            id = "openrouter/free",
            displayTitle = "Free Models Router",
            isFree = true,
            contextLength = 200_000,
            supportsSpeedModes = true
        ),
        SidePanelModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            displayTitle = "Llama 3.3 70B (free)",
            isFree = true,
            contextLength = 131_072
        ),
        SidePanelModel(
            id = "deepseek/deepseek-r1:free",
            displayTitle = "DeepSeek R1 (free)",
            isFree = true,
            contextLength = 163_840,
            supportsReasoning = true
        ),
        SidePanelModel(
            id = "google/gemini-2.0-flash-exp:free",
            displayTitle = "Gemini 2.0 Flash (free)",
            isFree = true,
            contextLength = 1_048_576
        )
    )

    private val openCodeModels = listOf(
        SidePanelModel(id = "gpt-4o-mini", displayTitle = "GPT-4o Mini"),
        SidePanelModel(id = "claude-3-5-haiku", displayTitle = "Claude 3.5 Haiku")
    )

    private val commandCodeModels = listOf(
        SidePanelModel(id = "default", displayTitle = "Command Code Default")
    )

    fun modelsFor(provider: SidePanelProviderApi): List<SidePanelModel> = when (provider.id) {
        SidePanelProviderApi.openRouter.id -> openRouterModels
        SidePanelProviderApi.openCode.id -> openCodeModels
        SidePanelProviderApi.commandCode.id -> commandCodeModels
        else -> openRouterModels
    }

    fun displayTitle(providerId: String?, modelId: String?): String {
        val provider = SidePanelProviderApi.resolve(providerId)
        val models = modelsFor(provider)
        return models.firstOrNull { it.id == modelId }?.displayTitle
            ?: models.firstOrNull()?.displayTitle
            ?: "Select model"
    }

    fun defaultModel(provider: SidePanelProviderApi): SidePanelModel =
        modelsFor(provider).first()
}
