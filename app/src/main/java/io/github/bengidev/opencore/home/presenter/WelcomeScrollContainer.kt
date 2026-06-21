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
    content: @Composable (viewportHeight: Dp) -> Unit,
    composer: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    var frozenViewportHeight by remember { mutableStateOf(0.dp) }
    var previousImeVisible by remember { mutableStateOf<Boolean?>(null) }

    // ponytail: Column + weight mirrors iOS safeAreaInset — composer outside scroll viewport
    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val measuredViewportHeight = maxHeight
            LaunchedEffect(imeVisible, measuredViewportHeight) {
                if (!imeVisible && measuredViewportHeight > 0.dp) {
                    frozenViewportHeight = measuredViewportHeight
                }
            }
            val viewportHeight = when {
                imeVisible && frozenViewportHeight > 0.dp -> frozenViewportHeight
                measuredViewportHeight > 0.dp -> measuredViewportHeight
                else -> frozenViewportHeight
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
                            if (viewportHeight > 0.dp) {
                                Modifier.heightIn(min = viewportHeight)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    content(viewportHeight)
                }

                Spacer(Modifier.height(1.dp))
            }
        }

        composer()
    }
}
