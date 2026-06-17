package io.github.bengidev.opencore.onboarding.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.onboarding.infrastructure.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class OnboardingComponent(
    componentContext: ComponentContext,
    private val repository: OnboardingRepository,
    private val onComplete: () -> Unit = {}
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(OnboardingState())
    val state: Value<OnboardingState> = _state

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }

        dispatch(OnboardingIntent.OnAppear)
        scope.launch {
            val completed = repository.isOnboardingCompleted()
            dispatch(OnboardingIntent.CompletionLoaded(completed))
            if (completed) onComplete()
        }
    }

    internal fun dispatch(intent: OnboardingIntent) {
        _state.update { current -> OnboardingReducer.reduce(current, intent) }
        when (intent) {
            is OnboardingIntent.FinishButtonTapped -> {
                scope.launch {
                    repository.completeOnboarding()
                    dispatch(OnboardingIntent.CompletionSaved)
                    onComplete()
                }
            }
            else -> Unit
        }
    }

    internal fun onNextTapped() = dispatch(OnboardingIntent.NextButtonTapped)
    internal fun onPreviousTapped() = dispatch(OnboardingIntent.PreviousButtonTapped)
    internal fun onPageSelected(index: Int) = dispatch(OnboardingIntent.PageSelected(index))
    internal fun onFinishTapped() = dispatch(OnboardingIntent.FinishButtonTapped)
    internal fun onSkipTapped() = dispatch(OnboardingIntent.SkipButtonTapped)
    internal fun onPromptChipTapped(index: Int) = dispatch(OnboardingIntent.PromptChipTapped(index))
    internal fun onAddQueuedPromptTapped() = dispatch(OnboardingIntent.AddQueuedPromptButtonTapped)
    internal fun onReasoningLevelChanged(value: Double) = dispatch(OnboardingIntent.ReasoningLevelChanged(value))
    internal fun onPairingToggleTapped() = dispatch(OnboardingIntent.PairingToggleTapped)
}
