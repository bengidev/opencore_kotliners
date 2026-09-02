package io.github.bengidev.opencore.onboarding.domain

import java.util.UUID

internal enum class OnboardingChatRole {
    USER,
    THINKING,
    ASSISTANT
}

internal data class OnboardingChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: OnboardingChatRole,
    val feature: OnboardingFeature?,
    val text: String,
    /** Pre-composed assistant payload for thinking rows — avoids layout work at morph time. */
    val preparedAssistant: OnboardingChatMessage? = null
) {
    fun morphToAssistant(): OnboardingChatMessage {
        preparedAssistant?.let { return it }
        val resolvedFeature = feature ?: return this
        return copy(
            role = OnboardingChatRole.ASSISTANT,
            text = resolvedFeature.accessibilitySummary,
            preparedAssistant = null
        )
    }

    companion object {
        fun user(prompt: String, feature: OnboardingFeature): OnboardingChatMessage =
            OnboardingChatMessage(
                role = OnboardingChatRole.USER,
                feature = feature,
                text = prompt
            )

        fun thinking(feature: OnboardingFeature): OnboardingChatMessage {
            val id = UUID.randomUUID().toString()
            return OnboardingChatMessage(
                id = id,
                role = OnboardingChatRole.THINKING,
                feature = feature,
                text = "Thinking…",
                preparedAssistant = OnboardingChatMessage(
                    id = id,
                    role = OnboardingChatRole.ASSISTANT,
                    feature = feature,
                    text = feature.accessibilitySummary
                )
            )
        }

        fun assistant(feature: OnboardingFeature): OnboardingChatMessage =
            OnboardingChatMessage(
                role = OnboardingChatRole.ASSISTANT,
                feature = feature,
                text = feature.accessibilitySummary
            )
    }
}
