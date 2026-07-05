package io.github.bengidev.opencore.chat.presenter

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.utilities.ChatRichContentSegment
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MarkdownEmbedWebView(
    segment: ChatRichContentSegment,
    palette: OpenCorePalette,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var contentHeightPx by remember(segment) { mutableIntStateOf(1) }
    val density = context.resources.displayMetrics.density
    val heightDp = (contentHeightPx / density).dp.coerceIn(48.dp, 480.dp)

    AndroidView(
        modifier = modifier
            .heightIn(min = 48.dp, max = 480.dp)
            .height(heightDp),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                isVerticalScrollBarEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onHeightChanged(height: Int) {
                            post { contentHeightPx = height }
                        }
                    },
                    "AndroidBridge",
                )
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/markdown-embed/host.html")
            }
        },
        update = { webView ->
            val themeJson = palette.toEmbedThemeJson()
            webView.evaluateJavascript("applyTheme('${themeJson.escapeForJsSingleQuotedString()}')", null)
            when (segment) {
                is ChatRichContentSegment.MermaidDiagram -> {
                    val escaped = segment.source.escapeForJsSingleQuotedString()
                    webView.evaluateJavascript("renderMermaid('$escaped')", null)
                }
                is ChatRichContentSegment.MathBlock -> {
                    val escaped = segment.latex.escapeForJsSingleQuotedString()
                    webView.evaluateJavascript("renderMath('$escaped')", null)
                }
                else -> Unit
            }
        },
    )
}

private fun OpenCorePalette.toEmbedThemeJson(): String =
    JSONObject()
        .put("background", "transparent")
        .put("text", argbHex(textPrimary))
        .put("muted", argbHex(textSecondary))
        .toString()

private fun argbHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun String.escapeForJsSingleQuotedString(): String =
    buildString(length) {
        for (ch in this@escapeForJsSingleQuotedString) {
            when (ch) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }
