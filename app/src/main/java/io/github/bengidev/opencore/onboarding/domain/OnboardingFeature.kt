package io.github.bengidev.opencore.onboarding.domain

import androidx.annotation.DrawableRes
import io.github.bengidev.opencore.R

internal data class OnboardingFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    @param:DrawableRes val imageRes: Int
) {
    companion object {
        val catalog: List<OnboardingFeature> = listOf(
            OnboardingFeature(
                id = "neural_core",
                title = "Intelligent Neural Core",
                subtitle = "On-device contextual reasoning with zero external latency.",
                description = "Understands your workspace context locally — no cloud round trips, no network lag. " +
                    "Models run on device so answers stay private and feel instant.",
                imageRes = R.drawable.onboarding_feature_neural_core
            ),
            OnboardingFeature(
                id = "spatial_canvas",
                title = "Dynamic Spatial Canvas",
                subtitle = "Multi-dimensional organization for fluid workspace mapping.",
                description = "Arrange notes, files, and threads in a spatial layout that mirrors how you think. " +
                    "Pan, cluster, and refocus without losing track of where anything lives.",
                imageRes = R.drawable.onboarding_feature_spatial_canvas
            ),
            OnboardingFeature(
                id = "workflows",
                title = "Autonomous Workflows",
                subtitle = "Self-healing pipelines that automate cross-tool tasks.",
                description = "Chain actions across apps with routines that recover on their own. " +
                    "Set triggers once and let background pipelines handle the repetitive work.",
                imageRes = R.drawable.onboarding_feature_workflows
            ),
            OnboardingFeature(
                id = "vault",
                title = "Encrypted Edge Vault",
                subtitle = "Zero-knowledge security anchored to device hardware.",
                description = "Keys are sealed in hardware-backed storage with zero-knowledge encryption. " +
                    "Your vault stays on-device — only you hold the keys.",
                imageRes = R.drawable.onboarding_feature_vault
            )
        )
    }
}
