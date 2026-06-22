package io.github.bengidev.opencore.sidepanel.domain

/** ponytail: static catalog until GET /models fetch lands. */
internal object SidePanelModelCatalog {
    private val openRouterModels = listOf(
        SidePanelModel(
            id = "openrouter/free",
            displayTitle = "Free Models Router"
        ),
        SidePanelModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            displayTitle = "Llama 3.3 70B (free)"
        ),
        SidePanelModel(
            id = "google/gemini-2.0-flash-exp:free",
            displayTitle = "Gemini 2.0 Flash (free)"
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
