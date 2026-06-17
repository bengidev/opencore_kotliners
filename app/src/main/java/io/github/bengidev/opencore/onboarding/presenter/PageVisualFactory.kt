package io.github.bengidev.opencore.onboarding.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.bengidev.opencore.onboarding.application.OnboardingState
import io.github.bengidev.opencore.onboarding.domain.OnboardingPage
import io.github.bengidev.opencore.onboarding.domain.OnboardingPageType
import io.github.bengidev.opencore.onboarding.presenter.visuals.EncryptedPairingVisualView
import io.github.bengidev.opencore.onboarding.presenter.visuals.IdeaStudioVisualView
import io.github.bengidev.opencore.onboarding.presenter.visuals.PromptQueueVisualView
import io.github.bengidev.opencore.onboarding.presenter.visuals.DefaultReasoningLevel
import io.github.bengidev.opencore.onboarding.presenter.visuals.ReasoningControlVisualView
import io.github.bengidev.opencore.onboarding.presenter.visuals.WorkspaceReadyVisualView

/**
 * Factory dispatcher that routes page types to their corresponding visual demo views.
 */
@Composable
internal fun PageVisualFactory(
    page: OnboardingPage,
    state: OnboardingState,
    appeared: Boolean,
    onPromptChipTapped: (Int) -> Unit,
    onPairingToggleTapped: () -> Unit,
    onReasoningLevelChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    when (page.type) {
        OnboardingPageType.EncryptedPairing -> {
            EncryptedPairingVisualView(
                pairingConfirmed = state.demoState.pairingConfirmed,
                onTogglePairing = onPairingToggleTapped,
                onActionButtonClick = onPairingToggleTapped,
                appeared = appeared,
                modifier = modifier
            )
        }

        OnboardingPageType.IdeaStudio -> {
            IdeaStudioVisualView(
                selectedPromptIndex = state.demoState.selectedPromptIndex,
                onPromptSelected = onPromptChipTapped,
                appeared = appeared,
                modifier = modifier
            )
        }

        OnboardingPageType.PromptQueue -> {
            PromptQueueVisualView(
                queuedPromptCount = state.demoState.queuedPromptCount,
                appeared = appeared,
                modifier = modifier
            )
        }

        OnboardingPageType.ReasoningControl -> {
            LaunchedEffect(page.id) {
                onReasoningLevelChanged(DefaultReasoningLevel.toDouble())
            }
            ReasoningControlVisualView(
                reasoningLevel = state.demoState.reasoningLevel.toFloat(),
                onReasoningLevelChanged = { onReasoningLevelChanged(it.toDouble()) },
                appeared = appeared,
                modifier = modifier
            )
        }

        OnboardingPageType.WorkspaceReady -> {
            WorkspaceReadyVisualView(
                page = page,
                appeared = appeared,
                modifier = modifier
            )
        }
    }
}
