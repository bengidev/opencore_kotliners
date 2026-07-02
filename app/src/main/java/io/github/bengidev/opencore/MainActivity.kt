package io.github.bengidev.opencore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelHistoryRepository
import io.github.bengidev.opencore.speech.SpeechFacade
import io.github.bengidev.opencore.speech.application.SpeechFlowController
import io.github.bengidev.opencore.vision.VisionFacade
import io.github.bengidev.opencore.vision.application.VisionFlowController
import io.github.bengidev.opencore.ui.decompose.rememberComponentContext
import io.github.bengidev.opencore.ui.theme.OpenCoreTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingFacade = OnboardingFacade()
        val homeFacade = HomeFacade()
        val sidePanelFacade = SidePanelFacade()
        val chatFacade = ChatFacade()
        val speechFacade = SpeechFacade()
        val visionFacade = VisionFacade()

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
                        speechFacade = speechFacade,
                        visionFacade = visionFacade,
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
    speechFacade: SpeechFacade,
    visionFacade: VisionFacade,
    activity: ComponentActivity,
    darkTheme: Boolean
) {
    val componentContext = rememberComponentContext()
    val scope = rememberCoroutineScope()
    var pendingPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingPermission?.complete(granted)
        pendingPermission = null
    }
    val history = remember(activity) { DataStoreSidePanelHistoryRepository(activity) }
    val preferenceStore = remember(activity) { DataStoreSidePanelPreferenceStore(activity) }
    val credentialStore = remember(activity) { CredentialEncryptedStore(activity) }
    val speechController: SpeechFlowController = remember(activity, scope, credentialStore, preferenceStore) {
        speechFacade.createController(
            context = activity,
            scope = scope,
            permissionRequester = {
                if (
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    true
                } else {
                    val deferred = CompletableDeferred<Boolean>()
                    pendingPermission = deferred
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    deferred.await()
                }
            },
            credentialStore = credentialStore,
            preferenceProvider = { preferenceStore.preference() },
        )
    }
    val visionController: VisionFlowController = remember(activity) {
        visionFacade.createController(context = activity)
    }
    val sidePanelComponent: SidePanelComponent = remember(componentContext, history, preferenceStore, credentialStore) {
        sidePanelFacade.createComponent(
            context = activity,
            componentContext = componentContext,
            history = history,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore
        )
    }
    val chatComponent: ChatComponent = remember(componentContext, history, preferenceStore, credentialStore) {
        chatFacade.createComponent(
            componentContext = componentContext,
            history = history,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore
        )
    }
    val homeComponent: HomeComponent = remember(componentContext, chatComponent, sidePanelComponent, preferenceStore, credentialStore) {
        facade.createComponent(
            componentContext = componentContext,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore,
            onSendMessage = { message, providerSortBy, reasoningEffort ->
                chatComponent.sendUserMessage(message, providerSortBy, reasoningEffort)
            },
            onNewConversation = {
                scope.launch {
                    speechController.cancelListening()
                    chatComponent.startNewConversation()
                }
            },
        )
    }

    LaunchedEffect(history) {
        history.pruneExpiredVoiceAttachments()
    }

    LaunchedEffect(chatComponent, sidePanelComponent, homeComponent) {
        chatComponent.onActiveConversationChanged = { id ->
            sidePanelComponent.session.setActiveConversationId(id)
        }
        chatComponent.onHistoryChanged = {
            sidePanelComponent.session.refreshConversationsIfVisible()
        }
        chatComponent.onConversationTitleChanged = { id, title ->
            sidePanelComponent.session.syncConversationTitle(id, title)
        }
        sidePanelComponent.onOpenConversation = { conversation ->
            scope.launch {
                speechController.cancelListening()
                chatComponent.openConversation(conversation)
            }
        }
        sidePanelComponent.onActiveConversationRenamed = chatComponent::onActiveConversationRenamed
        sidePanelComponent.onActiveConversationDeleted = chatComponent::onActiveConversationDeleted
        sidePanelComponent.onProviderChanged = { homeComponent.onProviderChanged() }
        sidePanelComponent.onCredentialsChanged = { homeComponent.onCredentialsChanged() }
    }

    HomeScreen(
        component = homeComponent,
        chatComponent = chatComponent,
        sidePanelComponent = sidePanelComponent,
        speechController = speechController,
        visionController = visionController,
        darkTheme = darkTheme
    )
}
