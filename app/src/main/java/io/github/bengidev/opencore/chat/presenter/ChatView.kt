package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.theme.OpenCoreChatTheme

/** Active conversation screen — title, error banner, and message thread. */
@Composable
internal fun ChatView(
    state: ChatState,
    onDismissKeyboard: () -> Unit,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {},
    showsContextUsageDismissScrim: Boolean = false,
    onDismissContextUsage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OpenCoreChatTheme {
        val palette = ChatTheme.palette
        val title = ChatViewTitlePolicy.resolve(state.headerTitle)

        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("chat-view"),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = palette.assistantBubbleText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp)
                        .testTag("chat-view-title")
                        .then(
                            if (ChatViewTitlePolicy.requiresHeadingSemantics(state.headerTitle)) {
                                Modifier.semantics { heading() }
                            } else {
                                Modifier
                            }
                        ),
                )
            }

            ChatThreadView(
                state = state,
                onDismissKeyboard = onDismissKeyboard,
                showsContextUsageDismissScrim = showsContextUsageDismissScrim,
                onDismissContextUsage = onDismissContextUsage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )

            ChatErrorBannerView(
                streamingStatus = state.streamingStatus,
                errorMessage = state.streamErrorMessage,
                onRetry = onRetry,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
