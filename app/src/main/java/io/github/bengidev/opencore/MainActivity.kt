package io.github.bengidev.opencore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.onboarding.OnboardingFacade
import io.github.bengidev.opencore.onboarding.OnboardingScreen
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.ui.theme.OpenCoreTheme

class MainActivity : ComponentActivity() {

    private val lifecycleRegistry = LifecycleRegistry()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleRegistry.resume()
        enableEdgeToEdge()

        val facade = OnboardingFacade()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }
            var showOnboarding by rememberSaveable { mutableStateOf(true) }

            val componentContext = remember { DefaultComponentContext(lifecycle = lifecycleRegistry) }
            val onboardingComponent: OnboardingComponent = remember {
                facade.createComponent(
                    context = this@MainActivity,
                    componentContext = componentContext,
                    onComplete = { showOnboarding = false }
                )
            }

            OpenCoreTheme(darkTheme = darkTheme) {
                if (showOnboarding) {
                    OnboardingScreen(
                        component = onboardingComponent,
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme }
                    )
                } else {
                    OpenCoreHomePlaceholder()
                }
            }
        }
    }
}
