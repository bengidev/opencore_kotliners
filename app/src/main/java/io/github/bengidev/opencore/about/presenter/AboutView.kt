package io.github.bengidev.opencore.about.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun AboutView(modifier: Modifier = Modifier) {
    val palette = HomeTheme.palette

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("about-view"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "OpenCore",
            color = palette.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Version 1.0",
            color = palette.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "OpenCore is an Android chat shell for exploring models through OpenAI-compatible providers.",
            color = palette.textSecondary,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
