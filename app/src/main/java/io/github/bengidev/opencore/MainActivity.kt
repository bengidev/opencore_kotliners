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
import com.arkivanov.decompose.childContext
import io.github.bengidev.opencore.about.AboutScreen
import io.github.bengidev.opencore.chat.ChatFacade
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.home.HomeFacade
import io.github.bengidev.opencore.home.HomeScreen
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.onboarding.OnboardingFacade
import io.github.bengidev.opencore.onboarding.OnboardingScreen
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.atoms.AtomsFacade
import io.github.bengidev.opencore.atoms.AtomsScreen
import io.github.bengidev.opencore.atoms.application.AtomsComponent
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.sidepanel.SidePanelSettingsScreen
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSettingsContextCompactionPreferenceStore
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import io.github.bengidev.opencore.shared.persistence.room.RoomAtomHistoryRepository
import io.github.bengidev.opencore.atoms.domain.toAtom
import io.github.bengidev.opencore.shared.tokenization.ContextTokenCounter
import io.github.bengidev.opencore.speech.SpeechFacade
import io.github.bengidev.opencore.speech.application.SpeechFlowController
import io.github.bengidev.opencore.tabbar.TabBarScreen
import io.github.bengidev.opencore.tabbar.domain.HomeTab
import io.github.bengidev.opencore.vision.VisionFacade
import io.github.bengidev.opencore.vision.application.VisionFlowController
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
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
        val atomsFacade = AtomsFacade()
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
                        atomsFacade = atomsFacade,
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
    atomsFacade: AtomsFacade,
    chatFacade: ChatFacade,
    speechFacade: SpeechFacade,
    visionFacade: VisionFacade,
    activity: ComponentActivity,
    darkTheme: Boolean
) {
    val componentContext = rememberComponentContext()
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    var pendingPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingPermission?.complete(granted)
        pendingPermission = null
    }
    val history = remember(activity) { RoomAtomHistoryRepository(activity) }
    val legacyHistory = remember(activity) { DataStoreSidePanelHistoryRepository(activity) }
    val preferenceStore = remember(activity) { DataStoreSidePanelPreferenceStore(activity) }
    val compactionPreferenceStore = remember(activity) {
        DataStoreSettingsContextCompactionPreferenceStore(activity)
    }
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
    val settingComponent: SidePanelSettingComponent = remember(componentContext, preferenceStore, credentialStore, compactionPreferenceStore) {
        SidePanelSettingComponent(
            componentContext = componentContext.childContext("setting"),
            credentialStore = credentialStore,
            preferenceStore = preferenceStore,
            compactionPreferenceStore = compactionPreferenceStore,
        )
    }
    val atomsComponent: AtomsComponent = remember(componentContext, history) {
        atomsFacade.createComponent(
            componentContext = componentContext.childContext("atoms"),
            history = history,
        )
    }
    val chatComponentHolder = remember { mutableStateOf<ChatComponent?>(null) }
    val homeComponent: HomeComponent = remember(componentContext, preferenceStore, credentialStore) {
        facade.createComponent(
            componentContext = componentContext,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore,
            onSendMessage = { message, providerSortBy, reasoningEffort ->
                chatComponentHolder.value?.sendUserMessage(message, providerSortBy, reasoningEffort)
            },
            onNewConversation = {
                scope.launch {
                    speechController.cancelListening()
                    chatComponentHolder.value?.startNewConversation()
                }
            },
        )
    }
    var historyMigrationReady by remember { mutableStateOf(false) }

    val chatComponent: ChatComponent = remember(
        componentContext,
        history,
        preferenceStore,
        credentialStore,
        compactionPreferenceStore,
        homeComponent,
    ) {
        chatFacade.createComponent(
            componentContext = componentContext.childContext("chat"),
            history = history,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore,
            compactionPreferenceStore = compactionPreferenceStore,
            contextLengthProvider = {
                val state = homeComponent.state.value
                state.selectedModelId?.let { id ->
                    state.availableModels.firstOrNull { it.id == id }?.contextLength
                }
            },
        ).also { chatComponentHolder.value = it }
    }

    LaunchedEffect(Unit) {
        ContextTokenCounter.warmUp()
        val legacyConversations = legacyHistory.listConversations().map { it.toAtom() }
        val legacyMessages = legacyConversations.associate { atom ->
            atom.id to legacyHistory.loadMessages(atom.id)
        }
        history.migrateFromDataStoreIfNeeded(legacyConversations, legacyMessages)
        history.pruneExpiredVoiceAttachments()
        historyMigrationReady = true
    }

    LaunchedEffect(chatComponent, atomsComponent, homeComponent) {
        chatComponent.onActiveConversationChanged = { id ->
            atomsComponent.setActiveAtomId(id)
        }
        chatComponent.onHistoryChanged = {
            atomsComponent.refreshIfNeeded()
        }
        chatComponent.onConversationTitleChanged = { id, title ->
            atomsComponent.syncAtomTitle(id, title)
        }
        atomsComponent.onOpenAtom = { conversation ->
            scope.launch {
                speechController.cancelListening()
                selectedTab = HomeTab.HOME
                chatComponent.openConversation(conversation)
            }
        }
        atomsComponent.onActiveAtomRenamed = chatComponent::onActiveConversationRenamed
        atomsComponent.onActiveAtomDeleted = chatComponent::onActiveConversationDeleted
        settingComponent.onProviderChanged = { homeComponent.onProviderChanged() }
        settingComponent.onCredentialsChanged = { homeComponent.onCredentialsChanged() }
    }

    OpenCoreHomeTheme(darkTheme = darkTheme) {
        if (!historyMigrationReady) {
            Box(modifier = Modifier.fillMaxSize())
            return@OpenCoreHomeTheme
        }
        TabBarScreen(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            homeContent = {
                HomeScreen(
                    component = homeComponent,
                    chatComponent = chatComponent,
                    speechController = speechController,
                    visionController = visionController,
                    darkTheme = darkTheme,
                    onConfigureApiKeyTapped = { selectedTab = HomeTab.SETTINGS },
                )
            },
            atomsContent = { AtomsScreen(component = atomsComponent) },
            settingsContent = { SidePanelSettingsScreen(component = settingComponent) },
            aboutContent = { AboutScreen() },
        )
    }
}
