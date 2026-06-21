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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WelcomeScrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable (viewportHeight: Dp) -> Unit,
    composer: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val imeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val keyboardController = LocalSoftwareKeyboardController.current
    var restingViewportHeight by remember { mutableStateOf(0.dp) }
    var peakImeBottomPx by remember { mutableIntStateOf(0) }

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
            val layoutViewportHeight = when {
                imeVisible && restingViewportHeight > 0.dp -> restingViewportHeight
                measuredViewportHeight > 0.dp -> measuredViewportHeight
                else -> restingViewportHeight
            }

            SideEffect {
                when {
                    imeBottomPx == 0 -> peakImeBottomPx = 0
                    imeBottomPx > peakImeBottomPx -> peakImeBottomPx = imeBottomPx
                }
            }

            LaunchedEffect(imeBottomPx, scrollState.maxValue, peakImeBottomPx) {
                val maxScroll = scrollState.maxValue
                when {
                    imeBottomPx == 0 -> {
                        if (scrollState.value != 0) scrollState.scrollTo(0)
                    }
                    maxScroll > 0 && peakImeBottomPx > 0 -> {
                        val target = (maxScroll * imeBottomPx.toFloat() / peakImeBottomPx)
                            .roundToInt()
                            .coerceIn(0, maxScroll)
                        if (scrollState.value != target) scrollState.scrollTo(target)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imeNestedScroll()
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
