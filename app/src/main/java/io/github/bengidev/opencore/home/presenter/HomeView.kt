package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.theme.HomeTheme

private val WelcomeComposerClearance = 180.dp

@Composable
internal fun HomeView(
    state: HomeState,
    onDraftMessageChanged: (String) -> Unit,
    onSidebarTapped: () -> Unit,
    onNewConversationTapped: () -> Unit,
    onAttachmentTapped: () -> Unit,
    onMicrophoneTapped: () -> Unit,
    onSendTapped: () -> Unit,
    onModelSelectorTapped: () -> Unit,
    onSpeedModeTapped: () -> Unit,
    onContextUsageTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceBase)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                onSidebarTapped = onSidebarTapped,
                onNewConversationTapped = onNewConversationTapped
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = WelcomeComposerClearance),
                contentAlignment = Alignment.TopCenter
            ) {
                HomeWelcomeView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        HomeComposerView(
            state = state,
            onDraftMessageChanged = onDraftMessageChanged,
            onAttachmentTapped = onAttachmentTapped,
            onMicrophoneTapped = onMicrophoneTapped,
            onSendTapped = onSendTapped,
            onModelSelectorTapped = onModelSelectorTapped,
            onSpeedModeTapped = onSpeedModeTapped,
            onContextUsageTapped = onContextUsageTapped,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 10.dp)
                .imePadding()
        )
    }
}
