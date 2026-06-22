package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.home.presenter.components.homeComposerGlass
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

@Composable
internal fun ChatThreadView(
    state: ChatState,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    if (state.messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .testTag("chat-thread-empty"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Start a conversation",
                style = typography.welcomeCaption,
                color = palette.textSecondary
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat-thread-list"),
        reverseLayout = false,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        items(state.messages, key = { it.id }) { message ->
            ChatMessageRow(message = message)
        }
    }
}

@Composable
private fun ChatMessageRow(message: SidePanelMessage) {
    val isUser = message.role == ChatMessageRole.USER
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat-message-row"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleModifier = Modifier
            .widthIn(max = 320.dp)
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 16.dp
            )
            .then(
                if (isUser) {
                    Modifier
                        .background(
                            color = palette.controlStrong,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("chat-message-bubble-user")
                } else {
                    Modifier
                        .homeComposerGlass(cornerRadius = 20.dp, shadowOpacity = 0.08f)
                        .testTag("chat-message-bubble-assistant")
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)

        Column(modifier = bubbleModifier) {
            Text(
                text = message.content,
                style = typography.composerBody,
                color = if (isUser) palette.controlStrongText else palette.textPrimary,
                modifier = Modifier.testTag("chat-message-text"),
                overflow = TextOverflow.Visible
            )
        }
    }
}
