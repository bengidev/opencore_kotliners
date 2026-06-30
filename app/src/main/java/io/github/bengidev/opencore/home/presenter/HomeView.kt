package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.presenter.ChatStreamingStatusCapsuleView
import io.github.bengidev.opencore.chat.presenter.ChatView
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.presenter.ChatVoiceNotePlaybackController
import io.github.bengidev.opencore.speech.application.SpeechFlowState
import io.github.bengidev.opencore.vision.application.VisionFlowState
import io.github.bengidev.opencore.home.theme.HomeTheme

private val ComposerBottomPadding = 10.dp

@Composable
internal fun HomeView(
    state: HomeState,
    chatState: ChatState,
    speechState: SpeechFlowState,
    visionState: VisionFlowState,
    draftAttachments: List<ChatMessageAttachment>,
    composerText: String,
    canSend: Boolean,
    voicePlaybackController: ChatVoiceNotePlaybackController,
    onDraftMessageChanged: (String) -> Unit,
    onSidebarTapped: () -> Unit,
    onNewConversationTapped: () -> Unit,
    onAttachmentTapped: () -> Unit,
    onRemoveAttachment: (java.util.UUID) -> Unit,
    onVisionErrorDismissed: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    onSpeechErrorDismissed: () -> Unit,
    onSendTapped: () -> Unit,
    onConfigureApiKeyTapped: () -> Unit,
    onModelSelectorTapped: () -> Unit,
    onSpeedModeSelected: (HomeComposerSpeedMode) -> Unit,
    onReasoningEffortSelected: (ModelReasoningEffort) -> Unit,
    onContextUsagePresentedChanged: (Boolean) -> Unit,
    onChatRetryTapped: () -> Unit = {},
    onChatErrorDismissed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val composer: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (chatState.showsStreamingStatusCapsule) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                ) {
                    ChatStreamingStatusCapsuleView()
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            HomeComposerView(
                state = state,
                speechState = speechState,
                visionState = visionState,
                draftAttachments = draftAttachments,
                composerText = composerText,
                canSend = canSend,
                isSending = chatState.isSending,
                isLoadingMessages = chatState.isLoadingMessages,
                onDraftMessageChanged = onDraftMessageChanged,
                onAttachmentTapped = {
                    dismissKeyboard()
                    onAttachmentTapped()
                },
                onRemoveAttachment = onRemoveAttachment,
                onVisionErrorDismissed = onVisionErrorDismissed,
                onStartVoiceInput = {
                    dismissKeyboard()
                    onStartVoiceInput()
                },
                onStopVoiceInput = {
                    dismissKeyboard()
                    onStopVoiceInput()
                },
                onCancelVoiceInput = {
                    dismissKeyboard()
                    onCancelVoiceInput()
                },
                onSpeechErrorDismissed = onSpeechErrorDismissed,
                onSendTapped = {
                    dismissKeyboard()
                    onSendTapped()
                },
                onConfigureApiKeyTapped = {
                    dismissKeyboard()
                    onConfigureApiKeyTapped()
                },
                onModelSelectorTapped = {
                    dismissKeyboard()
                    onModelSelectorTapped()
                },
                onSpeedModeSelected = {
                    dismissKeyboard()
                    onSpeedModeSelected(it)
                },
                onReasoningEffortSelected = {
                    dismissKeyboard()
                    onReasoningEffortSelected(it)
                },
                onContextUsagePresentedChanged = { presented ->
                    if (presented) dismissKeyboard()
                    onContextUsagePresentedChanged(presented)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = ComposerBottomPadding)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceBase)
            .navigationBarsPadding()
    ) {
        if (chatState.isThreadActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = HomeTopBarClearance)
                    .imePadding()
            ) {
                ChatView(
                    state = chatState,
                    voicePlaybackController = voicePlaybackController,
                    onDismissKeyboard = dismissKeyboard,
                    onRetry = onChatRetryTapped,
                    onDismiss = onChatErrorDismissed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .widthIn(max = 620.dp)
                        .padding(horizontal = 8.dp)
                )
                composer()
            }
        } else {
            WelcomeScrollContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = HomeTopBarClearance),
                content = { viewportHeight ->
                    HomeWelcomeView(
                        viewportHeight = viewportHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = 8.dp)
                    )
                },
                composer = composer
            )
        }

        HomeTopBarOverlay(
            onSidebarTapped = {
                dismissKeyboard()
                onSidebarTapped()
            },
            onNewConversationTapped = {
                dismissKeyboard()
                onNewConversationTapped()
            },
            onDismissKeyboard = dismissKeyboard,
            threadTitle = null,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
