package io.github.bengidev.opencore.onboarding

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.onboarding.presenter.OnboardingSinglePageScreen
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.onboarding.theme.OpenCoreOnboardingTheme

@Composable
internal fun OnboardingScreen(
    component: OnboardingComponent,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    OpenCoreOnboardingTheme(darkTheme = darkTheme) {
        val palette = OnboardingTheme.palette
        val backgroundColor by animateColorAsState(
            targetValue = palette.surfaceBase,
            animationSpec = tween(280),
            label = "OnboardingBackground"
        )
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.navigationBarColor = backgroundColor.toArgb()
                window.statusBarColor = backgroundColor.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            OnboardingSinglePageScreen(
                darkTheme = darkTheme,
                reduceMotion = reduceMotion,
                onThemeToggle = onThemeToggle,
                onComplete = { component.finish() }
            )
        }
    }
}
