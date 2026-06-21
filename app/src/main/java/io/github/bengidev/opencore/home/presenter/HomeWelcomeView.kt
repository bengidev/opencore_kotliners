package io.github.bengidev.opencore.home.presenter

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.presenter.visuals.HomeParticleOrbView
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.ui.components.ScaleToFitText

@Composable
internal fun HomeWelcomeView(
    viewportHeight: Dp,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette
    val layout = remember(viewportHeight) { HomeWelcomeLayoutMetrics.resolve(viewportHeight) }
    val topSpacer by animateDpAsState(
        targetValue = layout.topSpacer,
        animationSpec = tween(durationMillis = 200),
        label = "welcome-top-spacer"
    )
    val bottomSpacer by animateDpAsState(
        targetValue = layout.bottomSpacer,
        animationSpec = tween(durationMillis = 200),
        label = "welcome-bottom-spacer"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(topSpacer))

        HomeParticleOrbView(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.orbHeight)
                .padding(bottom = layout.orbBottomPadding)
        )

        ScaleToFitText(
            text = "Hi! How can I help you?",
            style = HomeTheme.typography.welcomeTitle,
            color = palette.textPrimary,
            maxLines = 1,
            minFontSize = 19.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Chats are end-to-end encrypted.",
            style = HomeTheme.typography.welcomeCaption,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your data is safe.",
            style = HomeTheme.typography.welcomeCaption,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(bottomSpacer))
    }
}

private data class HomeWelcomeLayoutMetrics(
    val topSpacer: Dp,
    val bottomSpacer: Dp,
    val orbHeight: Dp,
    val orbBottomPadding: Dp
) {
    companion object {
        private val heroTextBlockHeight = 66.dp
        private val minEdgeSpacing = 16.dp
        private val standardOrbHeight = 260.dp
        private val standardOrbPadding = 28.dp
        private val compactOrbHeight = 200.dp
        private val compactOrbPadding = 20.dp

        fun resolve(viewportHeight: Dp): HomeWelcomeLayoutMetrics {
            if (viewportHeight <= 0.dp) {
                return HomeWelcomeLayoutMetrics(
                    topSpacer = 72.dp,
                    bottomSpacer = 72.dp,
                    orbHeight = standardOrbHeight,
                    orbBottomPadding = standardOrbPadding
                )
            }

            centeredMetrics(
                viewportHeight = viewportHeight,
                orbHeight = standardOrbHeight,
                orbBottomPadding = standardOrbPadding
            )?.let { return it }

            val compactHeroHeight = compactOrbHeight + compactOrbPadding + heroTextBlockHeight
            val spacing = maxOf(minEdgeSpacing, (viewportHeight - compactHeroHeight) / 2)
            return HomeWelcomeLayoutMetrics(
                topSpacer = spacing,
                bottomSpacer = spacing,
                orbHeight = compactOrbHeight,
                orbBottomPadding = compactOrbPadding
            )
        }

        private fun centeredMetrics(
            viewportHeight: Dp,
            orbHeight: Dp,
            orbBottomPadding: Dp
        ): HomeWelcomeLayoutMetrics? {
            val heroHeight = orbHeight + orbBottomPadding + heroTextBlockHeight
            if (heroHeight > viewportHeight) return null
            val spacing = maxOf(minEdgeSpacing, (viewportHeight - heroHeight) / 2)
            return HomeWelcomeLayoutMetrics(
                topSpacer = spacing,
                bottomSpacer = spacing,
                orbHeight = orbHeight,
                orbBottomPadding = orbBottomPadding
            )
        }
    }
}
