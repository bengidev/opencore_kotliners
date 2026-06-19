package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.theme.HomeTheme
import kotlinx.coroutines.CancellationException

private val DefaultComposerClearance = 180.dp
private val ComposerClearancePadding = 12.dp

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
    val density = LocalDensity.current
    var composerClearance by remember { mutableStateOf(DefaultComposerClearance) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceBase)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                onSidebarTapped = onSidebarTapped,
                onNewConversationTapped = onNewConversationTapped,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { keyboardController?.hide() }
                }
            )

            WelcomeScrollContainer(
                composerClearance = composerClearance,
                content = { viewportHeight ->
                    HomeWelcomeView(
                        viewportHeight = viewportHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = 8.dp)
                    )
                },
                composer = {
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
                            .fillMaxWidth()
                            .widthIn(max = 620.dp)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 10.dp)
                            .imePadding()
                            .onSizeChanged { size ->
                                composerClearance = with(density) {
                                    size.height.toDp() + ComposerClearancePadding
                                }
                            }
                    )
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WelcomeScrollContainer(
    composerClearance: Dp,
    content: @Composable (viewportHeight: Dp) -> Unit,
    composer: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var restingViewportHeightPx by remember { mutableIntStateOf(0) }
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(imeVisible) {
        try {
            if (imeVisible) {
                scrollState.animateScrollTo(scrollState.maxValue)
            } else {
                scrollState.animateScrollTo(0)
            }
        } catch (_: CancellationException) {
            // Scroll animation cancelled — safe to ignore.
        }
    }

    val restingViewportHeightDp = with(density) { restingViewportHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .onSizeChanged { size ->
                    if (!imeVisible) restingViewportHeightPx = size.height
                }
                .pointerInput(Unit) {
                    detectTapGestures { keyboardController?.hide() }
                }
        ) {
            Spacer(Modifier.height(1.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = restingViewportHeightDp),
                contentAlignment = Alignment.TopCenter
            ) {
                content(restingViewportHeightDp)
            }

            Spacer(Modifier.height(1.dp))
            Spacer(Modifier.height(composerClearance))
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            composer()
        }
    }
}
