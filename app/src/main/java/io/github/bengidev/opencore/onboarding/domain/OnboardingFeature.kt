package io.github.bengidev.opencore.onboarding.domain

internal data class OnboardingFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val userPrompt: String,
    val iconName: String,
    val orbStyleIndex: Int
) {
    val accessibilitySummary: String
        get() = "$title. $subtitle"

    val feedAccessibilityLabel: String
        get() = "$title. $subtitle. $description"

    companion object {
        fun wrappedCatalogIndex(index: Int): Int {
            val count = catalog.size
            if (count == 0) return 0
            return ((index % count) + count) % count
        }

        val catalog: List<OnboardingFeature> = listOf(
            OnboardingFeature(
                id = "neural_core",
                title = "Intelligent Neural Core",
                subtitle = "On-device contextual reasoning with zero external latency.",
                description = "Understands your workspace context locally — no cloud round trips, no network lag. " +
                    "Models run on device so answers stay private and feel instant.",
                userPrompt = "How does on-device reasoning work?",
                iconName = "cpu",
                orbStyleIndex = 0
            ),
            OnboardingFeature(
                id = "spatial_canvas",
                title = "Dynamic Spatial Canvas",
                subtitle = "Multi-dimensional organization for fluid workspace mapping.",
                description = "Arrange notes, files, and threads in a spatial layout that mirrors how you think. " +
                    "Pan, cluster, and refocus without losing track of where anything lives.",
                userPrompt = "Can it map my workspace spatially?",
                iconName = "layers",
                orbStyleIndex = 1
            ),
            OnboardingFeature(
                id = "workflows",
                title = "Autonomous Workflows",
                subtitle = "Self-healing pipelines that automate cross-tool tasks.",
                description = "Chain actions across apps with routines that recover on their own. " +
                    "Set triggers once and let background pipelines handle the repetitive work.",
                userPrompt = "What about automating workflows?",
                iconName = "branch",
                orbStyleIndex = 2
            ),
            OnboardingFeature(
                id = "vault",
                title = "Encrypted Edge Vault",
                subtitle = "Zero-knowledge security anchored to device hardware.",
                description = "Keys are sealed in hardware-backed storage with zero-knowledge encryption. " +
                    "Your vault stays on-device — only you hold the keys.",
                userPrompt = "Is my data secure on-device?",
                iconName = "lock",
                orbStyleIndex = 3
            )
        )
    }
}
