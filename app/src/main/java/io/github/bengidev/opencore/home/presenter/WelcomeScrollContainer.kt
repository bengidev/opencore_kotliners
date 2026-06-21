package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WelcomeScrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable (viewportHeight: Dp) -> Unit,
    composer: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    var restingViewportHeight by remember { mutableStateOf(0.dp) }
    var previousImeVisible by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val measuredViewportHeight = maxHeight
            LaunchedEffect(imeVisible, measuredViewportHeight) {
                if (!imeVisible && measuredViewportHeight > 0.dp) {
                    restingViewportHeight = measuredViewportHeight
                }
            }
            // ponytail: freeze layout while IME open — matches iOS restingViewportHeight when composer focused
            val layoutViewportHeight = when {
                imeVisible && restingViewportHeight > 0.dp -> restingViewportHeight
                measuredViewportHeight > 0.dp -> measuredViewportHeight
                else -> restingViewportHeight
            }

            LaunchedEffect(imeVisible, scrollState.maxValue) {
                if (previousImeVisible == null) {
                    previousImeVisible = imeVisible
                    return@LaunchedEffect
                }
                when {
                    imeVisible && previousImeVisible == false -> {
                        if (scrollState.maxValue == 0) return@LaunchedEffect
                        previousImeVisible = true
                        try {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        } catch (_: CancellationException) {
                            // Scroll animation cancelled — safe to ignore.
                        }
                    }
                    !imeVisible && previousImeVisible == true -> {
                        previousImeVisible = false
                        try {
                            scrollState.animateScrollTo(0)
                        } catch (_: CancellationException) {
                            // Scroll animation cancelled — safe to ignore.
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { keyboardController?.hide() }
                    }
            ) {
                Spacer(Modifier.height(1.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (layoutViewportHeight > 0.dp) {
                                Modifier.heightIn(min = layoutViewportHeight)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    content(layoutViewportHeight)
                }

                Spacer(Modifier.height(1.dp))
            }
        }

        composer()
    }
}
