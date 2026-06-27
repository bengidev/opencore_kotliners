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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WelcomeScrollContainer(
    modifier: Modifier = Modifier,
    showsContextUsageDismissScrim: Boolean = false,
    onDismissContextUsage: () -> Unit = {},
    content: @Composable (viewportHeight: Dp) -> Unit,
    composer: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val imeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    var restingViewportHeight by remember { mutableStateOf(0.dp) }
    var peakImeBottomPx by remember { mutableIntStateOf(0) }
    var readyForImeScroll by remember { mutableStateOf(false) }
    val reduceMotion = HomeContextUsagePopoverMotion.rememberReduceMotion()

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
                    readyForImeScroll = true
                }
            }
            val layoutViewportHeight = when {
                imeVisible && restingViewportHeight > 0.dp -> restingViewportHeight
                measuredViewportHeight > 0.dp -> measuredViewportHeight
                else -> restingViewportHeight
            }

            LaunchedEffect(imeBottomPx, scrollState.maxValue, readyForImeScroll) {
                if (!readyForImeScroll) return@LaunchedEffect

                if (imeBottomPx == 0) {
                    peakImeBottomPx = 0
                } else if (imeBottomPx > peakImeBottomPx) {
                    peakImeBottomPx = imeBottomPx
                }

                val target = welcomeImeScrollTarget(
                    imeBottomPx = imeBottomPx,
                    peakImeBottomPx = peakImeBottomPx,
                    maxScroll = scrollState.maxValue,
                    currentScroll = scrollState.value
                )
                if (target != null && scrollState.value != target) {
                    scrollState.scrollTo(target)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imeNestedScroll()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { dismissKeyboard() }
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

            if (showsContextUsageDismissScrim) {
                HomeContextUsageDismissScrim(
                    reduceMotion = reduceMotion,
                    onDismiss = onDismissContextUsage,
                )
            }
        }

        composer()
    }
}

internal fun welcomeImeScrollTarget(
    imeBottomPx: Int,
    peakImeBottomPx: Int,
    maxScroll: Int,
    currentScroll: Int
): Int? = when {
    imeBottomPx == 0 -> if (currentScroll == 0) null else 0
    maxScroll > 0 && peakImeBottomPx > 0 -> {
        (maxScroll * imeBottomPx.toFloat() / peakImeBottomPx)
            .roundToInt()
            .coerceIn(0, maxScroll)
    }
    else -> null
}
