package io.github.bengidev.opencore.onboarding.presenter.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.presenter.ChatThreadScrollPolicy
import io.github.bengidev.opencore.onboarding.domain.OnboardingChatMessage
import io.github.bengidev.opencore.onboarding.domain.OnboardingChatRole
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeature
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private const val MessageSpacingDp = 10
private const val MaxVisibleItems = 20
private const val FeedBottomInsetDp = 16
private const val MorphRevealSettleMs = 180L
private const val FeedEdgeFadeFraction = 0.10f

private val FeedItemEnterSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 175f
)

private val FeedItemSlideSpring = spring<IntOffset>(
    dampingRatio = 0.8f,
    stiffness = 175f
)

private object OnboardingChatFeedTiming {
    const val FirstMessageDelayMs = 350L
    const val AfterUserDelayMs = 450L
    const val ThinkingDurationMs = 1_100L
    const val AfterAssistantDelayMs = 1_300L
}

private enum class FeedStep {
    USER,
    THINKING,
    MORPH
}

@Composable
internal fun OnboardingFeatureChatFeedView(
    isActive: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = maxWidth

        if (reduceMotion) {
            StaticConversation(
                isActive = isActive,
                containerWidth = containerWidth
            )
            return@BoxWithConstraints
        }

        val palette = OnboardingTheme.palette
        val feedItems = remember { mutableStateListOf<OnboardingChatMessage>() }
        var nextFeatureIndex by remember { mutableIntStateOf(0) }
        var accessibilityFeatureIndex by remember { mutableIntStateOf(0) }
        var feedStep by remember { mutableStateOf(FeedStep.USER) }
        val listState = rememberLazyListState()

        LaunchedEffect(isActive) {
            if (!isActive) return@LaunchedEffect

            feedItems.clear()
            feedStep = FeedStep.USER
            nextFeatureIndex = 0
            accessibilityFeatureIndex = 0

            delay(OnboardingChatFeedTiming.FirstMessageDelayMs)

            while (isActive) {
                val catalog = OnboardingFeature.catalog
                if (catalog.isEmpty()) break

                val feature = catalog[OnboardingFeature.wrappedCatalogIndex(nextFeatureIndex)]

                when (feedStep) {
                    FeedStep.USER -> {
                        accessibilityFeatureIndex = nextFeatureIndex
                        feedItems.add(OnboardingChatMessage.user(feature.userPrompt, feature))
                        trimFeedIfNeeded(feedItems)
                        feedStep = FeedStep.THINKING
                        delay(OnboardingChatFeedTiming.AfterUserDelayMs)
                    }

                    FeedStep.THINKING -> {
                        feedItems.add(OnboardingChatMessage.thinking(feature))
                        trimFeedIfNeeded(feedItems)
                        feedStep = FeedStep.MORPH
                        delay(OnboardingChatFeedTiming.ThinkingDurationMs)
                    }

                    FeedStep.MORPH -> {
                        val thinkingIndex = feedItems.indexOfLast { it.role == OnboardingChatRole.THINKING }
                        if (thinkingIndex >= 0) {
                            // Assistant payload was prepared when thinking started; wait one
                            // frame so its shell height is already reserved before reveal.
                            withFrameNanos { }
                            feedItems[thinkingIndex] = feedItems[thinkingIndex].morphToAssistant()
                            withFrameNanos { }
                        }
                        nextFeatureIndex = (nextFeatureIndex + 1) % catalog.size
                        feedStep = FeedStep.USER
                        delay(OnboardingChatFeedTiming.AfterAssistantDelayMs)
                    }
                }
            }
        }

        LaunchedEffect(listState, feedItems.size) {
            snapshotFlow {
                feedItems.size to feedItems.lastOrNull()?.role
            }
                .distinctUntilChanged()
                .collect { (count, _) ->
                    if (count == 0) return@collect
                    val targetIndex = count - 1
                    val lastRole = feedItems.lastOrNull()?.role
                    withFrameNanos { }
                    if (lastRole == OnboardingChatRole.ASSISTANT) {
                        delay(MorphRevealSettleMs)
                        withFrameNanos { }
                    }
                    scrollFeedToBottom(
                        listState = listState,
                        targetIndex = targetIndex,
                        animate = OnboardingChatFeedScrollPolicy.shouldAnimateScroll(count)
                    )
                }
        }

        val accessibilityLabel = remember(accessibilityFeatureIndex) {
            val catalog = OnboardingFeature.catalog
            if (catalog.isEmpty()) {
                "Onboarding features"
            } else {
                catalog[OnboardingFeature.wrappedCatalogIndex(accessibilityFeatureIndex)].feedAccessibilityLabel
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = accessibilityLabel
                    },
                contentPadding = PaddingValues(top = 6.dp, bottom = FeedBottomInsetDp.dp),
                verticalArrangement = Arrangement.spacedBy(MessageSpacingDp.dp)
            ) {
                items(
                    items = feedItems,
                    key = { it.id }
                ) { message ->
                    FeedMessageItem(
                        message = message,
                        containerWidth = containerWidth,
                        reduceMotion = reduceMotion
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(FeedEdgeFadeFraction)
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to palette.surfaceBase,
                            1f to Color.Transparent
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun FeedMessageItem(
    message: OnboardingChatMessage,
    containerWidth: Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val visibleState = remember(message.id) {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier.clipToBounds(),
        enter = chatEnterTransition(message.role),
        exit = fadeOut(animationSpec = FeedItemEnterSpring)
    ) {
        OnboardingChatBubbleView(
            message = message,
            containerWidth = containerWidth,
            reduceMotion = reduceMotion
        )
    }
}

@Composable
private fun StaticConversation(
    isActive: Boolean,
    containerWidth: Dp
) {
    val palette = OnboardingTheme.palette
    var focusedFeatureIndex by remember { mutableIntStateOf(0) }
    val catalog = OnboardingFeature.catalog
    val feature = catalog.getOrNull(OnboardingFeature.wrappedCatalogIndex(focusedFeatureIndex))

    val accessibilityLabel = remember(focusedFeatureIndex) {
        feature?.feedAccessibilityLabel ?: "Onboarding features"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = accessibilityLabel },
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(MessageSpacingDp.dp)
        ) {
            if (feature != null) {
                item(key = "user-${feature.id}") {
                    OnboardingChatBubbleView(
                        message = OnboardingChatMessage.user(feature.userPrompt, feature),
                        containerWidth = containerWidth,
                        reduceMotion = true
                    )
                }
                item(key = "assistant-${feature.id}") {
                    OnboardingChatBubbleView(
                        message = OnboardingChatMessage.assistant(feature),
                        containerWidth = containerWidth,
                        reduceMotion = true
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(FeedEdgeFadeFraction)
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to palette.surfaceBase,
                        1f to Color.Transparent
                    )
                )
            )
        }
    }

    LaunchedEffect(isActive, catalog.size) {
        if (!isActive || catalog.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(4_000)
            focusedFeatureIndex = (focusedFeatureIndex + 1) % catalog.size
        }
    }
}

private fun trimFeedIfNeeded(feedItems: MutableList<OnboardingChatMessage>) {
    if (feedItems.size <= MaxVisibleItems) return
    val removeCount = feedItems.size - MaxVisibleItems
    repeat(removeCount) {
        feedItems.removeAt(0)
    }
}

private fun chatEnterTransition(role: OnboardingChatRole) =
    when (role) {
        OnboardingChatRole.USER -> slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
            animationSpec = FeedItemSlideSpring
        ) + fadeIn(FeedItemEnterSpring) + scaleIn(
            initialScale = 0.97f,
            animationSpec = FeedItemEnterSpring
        )

        OnboardingChatRole.THINKING,
        OnboardingChatRole.ASSISTANT -> slideInHorizontally(
            initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
            animationSpec = FeedItemSlideSpring
        ) + fadeIn(FeedItemEnterSpring) + scaleIn(
            initialScale = 0.97f,
            animationSpec = FeedItemEnterSpring
        )
    }

private object OnboardingChatFeedScrollPolicy {
    fun shouldAnimateScroll(messageCount: Int): Boolean = messageCount <= 3
}

/** Bottom-anchor scroll — mirrors iOS `scrollTo(_, anchor: .bottom)`. */
private suspend fun scrollFeedToBottom(
    listState: LazyListState,
    targetIndex: Int,
    animate: Boolean
) {
    if (targetIndex < 0) return

    snapshotFlow {
        listState.layoutInfo.totalItemsCount to listState.isScrollInProgress
    }
        .filter { (count, scrolling) ->
            count > targetIndex && !ChatThreadScrollPolicy.shouldDeferForActiveScroll(scrolling)
        }
        .first()

    val scrollOffset = Int.MAX_VALUE
    try {
        if (animate) {
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = scrollOffset
            )
        } else {
            listState.scrollToItem(
                index = targetIndex,
                scrollOffset = scrollOffset
            )
        }
    } catch (_: IllegalArgumentException) {
        // Layout race while items are still measuring.
    } catch (_: IllegalStateException) {
        delay(32L)
        try {
            if (animate) {
                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = scrollOffset
                )
            } else {
                listState.scrollToItem(
                    index = targetIndex,
                    scrollOffset = scrollOffset
                )
            }
        } catch (_: IllegalArgumentException) {
        } catch (_: IllegalStateException) {
            // Concurrent scroll or user drag — safe to ignore.
        }
    }
}
