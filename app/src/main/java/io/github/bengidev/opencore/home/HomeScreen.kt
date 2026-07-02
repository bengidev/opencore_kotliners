package io.github.bengidev.opencore.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.presenter.ChatVoiceNotePlaybackController
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentStore
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.home.presenter.HomeModelPickerSheet
import io.github.bengidev.opencore.home.presenter.HomeView
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
import io.github.bengidev.opencore.home.utilities.HomeComposerModelCapabilityLogic
import io.github.bengidev.opencore.sidepanel.SidePanelScreen
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.speech.application.SpeechFlowController
import io.github.bengidev.opencore.vision.application.VisionFlowController
import io.github.bengidev.opencore.vision.utilities.ContentUriMetadata
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    component: HomeComponent,
    chatComponent: ChatComponent,
    sidePanelComponent: SidePanelComponent,
    speechController: SpeechFlowController,
    visionController: VisionFlowController,
    darkTheme: Boolean,
) {
    val state by component.state.subscribeAsState()
    val chatState by chatComponent.state.subscribeAsState()
    val speechState by speechController.state.collectAsState()
    val visionState by visionController.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val voicePlaybackController = remember(scope) { ChatVoiceNotePlaybackController(scope) }

    DisposableEffect(voicePlaybackController) {
        onDispose { voicePlaybackController.release() }
    }

    var isAttachmentMenuVisible by remember { mutableStateOf(false) }
    var capabilityWarningMessage by remember { mutableStateOf<String?>(null) }

    val composerText = state.draftMessage
    val draftAttachments = chatState.draftAttachments
    val selectedModel = state.selectedModelId?.let { id ->
        state.availableModels.firstOrNull { it.id == id }
    }
    val canSend = state.canSendBase &&
        (state.draftMessage.isNotBlank() || draftAttachments.isNotEmpty()) &&
        !speechState.isListening &&
        !speechState.isTranscribing

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val resolved = ContentUriMetadata.resolve(context, uri)
            val attachment = visionController.attachUri(
                uri = uri,
                filename = resolved.filename,
                mimeType = resolved.mimeType,
            )
            attachment?.let { addImportedAttachment(it, selectedModel, state.selectedModelTitle, chatComponent) { msg ->
                capabilityWarningMessage = msg
            } }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val resolved = ContentUriMetadata.resolve(context, uri)
            val attachment = visionController.attachUri(
                uri = uri,
                filename = resolved.filename,
                mimeType = resolved.mimeType,
            )
            attachment?.let { addImportedAttachment(it, selectedModel, state.selectedModelTitle, chatComponent) { msg ->
                capabilityWarningMessage = msg
            } }
        }
    }

    LaunchedEffect(speechState.pendingCapture) {
        val capture = speechState.pendingCapture ?: return@LaunchedEffect
        applyVoiceCapture(
            capture = capture,
            existingDraft = state.draftMessage,
            onDraftChanged = component::onDraftMessageChanged,
        )
        speechController.clearPendingCapture()
    }

    LaunchedEffect(
        chatState.messages.size,
        chatState.streamingStatus,
        state.draftMessage,
        draftAttachments,
        state.selectedModelId,
        state.availableModels,
    ) {
        if (chatState.streamingStatus == ChatStreamingStatus.Running) return@LaunchedEffect
        component.refreshContextUsage(chatState.messages, draftAttachments)
    }

    capabilityWarningMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { capabilityWarningMessage = null },
            title = { Text("Attachment not supported") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { capabilityWarningMessage = null }) {
                    Text("OK")
                }
            },
        )
    }

    if (isAttachmentMenuVisible) {
        AlertDialog(
            onDismissRequest = { isAttachmentMenuVisible = false },
            title = { Text("Add attachment") },
            text = { Text("Attach a file or photo to include with your message.") },
            confirmButton = {
                TextButton(onClick = {
                    isAttachmentMenuVisible = false
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) { Text("Photo Library") }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAttachmentMenuVisible = false
                    filePickerLauncher.launch(arrayOf("image/*", "video/*", "text/*", "application/json"))
                }) { Text("Import File") }
            },
        )
    }

    OpenCoreHomeTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeView(
                state = state,
                chatState = chatState,
                speechState = speechState,
                visionState = visionState,
                draftAttachments = draftAttachments,
                composerText = composerText,
                canSend = canSend,
                voicePlaybackController = voicePlaybackController,
                onDraftMessageChanged = component::onDraftMessageChanged,
                onSidebarTapped = sidePanelComponent::toggleSidebar,
                onNewConversationTapped = component::onNewConversationTapped,
                onAttachmentTapped = { isAttachmentMenuVisible = true },
                onRemoveAttachment = chatComponent::removeDraftAttachment,
                onVisionErrorDismissed = visionController::clearError,
                onStartVoiceInput = { speechController.startListening() },
                onStopVoiceInput = {
                    scope.launch { speechController.stopListening() }
                },
                onCancelVoiceInput = {
                    scope.launch { speechController.cancelListening() }
                },
                onSpeechErrorDismissed = speechController::clearError,
                onSendTapped = {
                    when (
                        val decision = HomeComposerModelCapabilityLogic.validateDraft(
                            attachments = draftAttachments,
                            model = selectedModel,
                            modelName = state.selectedModelTitle,
                        )
                    ) {
                        HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Allowed -> {
                            visionController.dismissProcessingPresentation()
                            component.onSendTapped(draftAttachments)
                        }
                        is HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Blocked ->
                            capabilityWarningMessage = decision.message
                    }
                },
                onConfigureApiKeyTapped = sidePanelComponent::settingsButtonTapped,
                onModelSelectorTapped = component::onModelSelectorTapped,
                onSpeedModeSelected = component::onSpeedModeSelected,
                onReasoningEffortSelected = component::onReasoningEffortSelected,
                onContextUsagePresentedChanged = component::onContextUsagePresentedChanged,
                onChatRetryTapped = {
                    chatComponent.retry(state.activeProviderSortBy, state.activeReasoningEffort)
                },
                onChatErrorDismissed = chatComponent::dismissError,
            )
            SidePanelScreen(component = sidePanelComponent)
            HomeModelPickerSheet(
                state = state,
                onDismiss = component::onModelPickerDismissed,
                onModelSelected = component::onModelSelected,
                onSearchQueryChanged = component::onModelSearchQueryChanged,
                onFilterFreeOnlyChanged = component::onModelFilterFreeOnlyChanged,
            )
        }
    }
}

private fun applyVoiceCapture(
    capture: io.github.bengidev.opencore.speech.domain.SpeechCaptureResult,
    existingDraft: String,
    onDraftChanged: (String) -> Unit,
) {
    val transcript = capture.composerText.trim()
    if (transcript.isEmpty()) return
    val existing = existingDraft.trim()
    onDraftChanged(
        if (existing.isEmpty()) {
            transcript
        } else {
            SpeechFlowController.mergedDraft(existing, transcript)
        },
    )
}

private fun addImportedAttachment(
    attachment: ChatMessageAttachment,
    model: SidePanelModel?,
    modelName: String,
    chatComponent: ChatComponent,
    onBlocked: (String) -> Unit,
) {
    when (val decision = HomeComposerModelCapabilityLogic.validateNewAttachment(attachment, model, modelName)) {
        HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Allowed ->
            chatComponent.addDraftAttachment(attachment)
        is HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Blocked -> {
            ChatAttachmentStore.remove(attachment.localPath)
            onBlocked(decision.message)
        }
    }
}
