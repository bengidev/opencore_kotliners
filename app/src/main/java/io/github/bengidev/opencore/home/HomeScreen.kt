package io.github.bengidev.opencore.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.home.presenter.HomeModelPickerSheet
import io.github.bengidev.opencore.home.presenter.HomeView
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
import io.github.bengidev.opencore.sidepanel.SidePanelScreen
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent

@Composable
internal fun HomeScreen(
    component: HomeComponent,
    chatComponent: ChatComponent,
    sidePanelComponent: SidePanelComponent,
    darkTheme: Boolean
) {
    val state by component.state.subscribeAsState()
    val chatState by chatComponent.state.subscribeAsState()

    LaunchedEffect(
        chatState.messages.size,
        chatState.streamingStatus,
        state.draftMessage,
        state.selectedModelId,
        state.availableModels
    ) {
        if (chatState.streamingStatus == ChatStreamingStatus.Running) return@LaunchedEffect
        component.refreshContextUsage(chatState.messages)
    }

    OpenCoreHomeTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeView(
                state = state,
                chatState = chatState,
                onDraftMessageChanged = component::onDraftMessageChanged,
                onSidebarTapped = sidePanelComponent::toggleSidebar,
                onNewConversationTapped = component::onNewConversationTapped,
                onAttachmentTapped = component::onAttachmentTapped,
                onMicrophoneTapped = component::onMicrophoneTapped,
                onSendTapped = component::onSendTapped,
                onConfigureApiKeyTapped = sidePanelComponent::settingsButtonTapped,
                onModelSelectorTapped = component::onModelSelectorTapped,
                onSpeedModeSelected = component::onSpeedModeSelected,
                onReasoningEffortSelected = component::onReasoningEffortSelected,
                onChatRetryTapped = {
                    chatComponent.retry(state.activeProviderSortBy, state.activeReasoningEffort)
                },
                onChatErrorDismissed = chatComponent::dismissError
            )
            SidePanelScreen(component = sidePanelComponent)
            HomeModelPickerSheet(
                state = state,
                onDismiss = component::onModelPickerDismissed,
                onModelSelected = component::onModelSelected,
                onSearchQueryChanged = component::onModelSearchQueryChanged,
                onFilterFreeOnlyChanged = component::onModelFilterFreeOnlyChanged
            )
        }
    }
}
