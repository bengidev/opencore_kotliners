package io.github.bengidev.opencore.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.onboarding.presenter.FeaturePageView
import io.github.bengidev.opencore.onboarding.presenter.components.DiagonalHatchPattern
import io.github.bengidev.opencore.onboarding.presenter.components.OnboardingBottomNavigation
import io.github.bengidev.opencore.onboarding.presenter.components.OnboardingTopBar
import io.github.bengidev.opencore.onboarding.presenter.components.PixelGridBackground
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.onboarding.theme.OpenCoreOnboardingTheme

/** Main onboarding screen — layout matched to openzone-swifters OnboardingView. */
@Composable
internal fun OnboardingScreen(
    component: OnboardingComponent,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val state by component.state.subscribeAsState()

    OpenCoreOnboardingTheme(darkTheme = darkTheme) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(OnboardingTheme.palette.surfaceBase)
        ) {
            val screenHeight = maxHeight
            val compactHeight = screenHeight < 760.dp
            val horizontalPadding = (maxWidth * 0.055f).coerceIn(20.dp, 36.dp)

            PixelGridBackground(
                modifier = Modifier.fillMaxSize(),
                spacing = if (compactHeight) 18.dp else 22.dp,
                color = OnboardingTheme.palette.textTertiary.copy(
                    alpha = if (OnboardingTheme.palette.isDark) 0.06f else 0.04f
                )
            )
            DiagonalHatchPattern(
                modifier = Modifier.fillMaxSize(),
                color = OnboardingTheme.palette.lineSoft.copy(
                    alpha = if (OnboardingTheme.palette.isDark) 0.08f else 0.03f
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = horizontalPadding)
                    .padding(top = if (compactHeight) 8.dp else 12.dp, bottom = 18.dp)
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingTopBar(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    onSkip = component::onSkipTapped,
                    onThemeToggle = onThemeToggle
                )

                Spacer(modifier = Modifier.height(if (compactHeight) 12.dp else 18.dp))

                AnimatedContent(
                    targetState = state.currentPage,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "PageTransition",
                    contentAlignment = Alignment.TopCenter
                ) { pageIndex ->
                    val page = state.pages[pageIndex]

                    FeaturePageView(
                        page = page,
                        state = state,
                        compactHeight = compactHeight,
                        availableHeight = screenHeight,
                        onPromptChipTapped = component::onPromptChipTapped,
                        onPairingToggleTapped = component::onPairingToggleTapped,
                        onAddQueuedPromptTapped = component::onAddQueuedPromptTapped,
                        onReasoningLevelChanged = component::onReasoningLevelChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(if (compactHeight) 12.dp else 18.dp))

                OnboardingBottomNavigation(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    isLastPage = state.isLastPage,
                    onNext = component::onNextTapped,
                    onPrevious = component::onPreviousTapped,
                    onFinish = component::onFinishTapped,
                    onPageSelected = component::onPageSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
