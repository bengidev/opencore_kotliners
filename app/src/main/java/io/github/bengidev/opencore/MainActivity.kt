package io.github.bengidev.opencore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.bengidev.opencore.chat.ChatFacade
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.home.HomeFacade
import io.github.bengidev.opencore.home.HomeScreen
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.onboarding.OnboardingFacade
import io.github.bengidev.opencore.onboarding.OnboardingScreen
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.sidepanel.SidePanelFacade
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.ui.decompose.rememberComponentContext
import io.github.bengidev.opencore.ui.theme.OpenCoreTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingFacade = OnboardingFacade()
        val homeFacade = HomeFacade()
        val sidePanelFacade = SidePanelFacade()
        val chatFacade = ChatFacade()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                if (showOnboarding == null) {
                    showOnboarding = !onboardingFacade.isOnboardingCompleted(this@MainActivity)
                }
            }

            OpenCoreTheme(darkTheme = darkTheme) {
                when (showOnboarding) {
                    null -> Box(modifier = Modifier.fillMaxSize())
                    true -> OnboardingRoute(
                        facade = onboardingFacade,
                        activity = this@MainActivity,
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onComplete = { showOnboarding = false }
                    )
                    false -> HomeRoute(
                        facade = homeFacade,
                        sidePanelFacade = sidePanelFacade,
                        chatFacade = chatFacade,
                        activity = this@MainActivity,
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingRoute(
    facade: OnboardingFacade,
    activity: ComponentActivity,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onComplete: () -> Unit
) {
    val componentContext = rememberComponentContext()
    val onboardingComponent: OnboardingComponent = remember(componentContext) {
        facade.createComponent(
            context = activity,
            componentContext = componentContext,
            onComplete = onComplete
        )
    }

    OnboardingScreen(
        component = onboardingComponent,
        darkTheme = darkTheme,
        onThemeToggle = onThemeToggle
    )
}

@Composable
private fun HomeRoute(
    facade: HomeFacade,
    sidePanelFacade: SidePanelFacade,
    chatFacade: ChatFacade,
    activity: ComponentActivity,
    darkTheme: Boolean
) {
    val componentContext = rememberComponentContext()
    val history = remember { InMemorySidePanelHistoryRepository() }
    val sidePanelComponent: SidePanelComponent = remember(componentContext, history) {
        sidePanelFacade.createComponent(
            context = activity,
            componentContext = componentContext,
            history = history
        )
    }
    val chatComponent: ChatComponent = remember(componentContext, history) {
        chatFacade.createComponent(
            componentContext = componentContext,
            history = history
        )
    }
    val homeComponent: HomeComponent = remember(componentContext, chatComponent, sidePanelComponent) {
        facade.createComponent(
            componentContext = componentContext,
            onSendMessage = chatComponent::sendUserMessage,
            onNewConversation = chatComponent::startNewConversation
        )
    }

    LaunchedEffect(chatComponent, sidePanelComponent) {
        chatComponent.onActiveConversationChanged = { id ->
            sidePanelComponent.session.setActiveConversationId(id)
        }
        chatComponent.onHistoryChanged = {
            sidePanelComponent.session.refreshConversationsIfVisible()
        }
        sidePanelComponent.onOpenConversation = chatComponent::openConversation
        sidePanelComponent.onActiveConversationRenamed = chatComponent::onActiveConversationRenamed
        sidePanelComponent.onActiveConversationDeleted = chatComponent::onActiveConversationDeleted
    }

    HomeScreen(
        component = homeComponent,
        chatComponent = chatComponent,
        sidePanelComponent = sidePanelComponent,
        darkTheme = darkTheme
    )
}
