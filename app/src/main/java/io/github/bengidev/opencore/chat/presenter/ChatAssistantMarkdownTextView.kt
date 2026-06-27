package io.github.bengidev.opencore.chat.presenter

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatAssistantMarkdownRenderer
import io.github.bengidev.opencore.home.theme.HomeTheme

/** Completed assistant answer text with markdown styling. */
@Composable
internal fun ChatAssistantMarkdownTextView(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val typography = ChatTheme.typography

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(true)
                isFocusable = false
                isClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, typography.assistantMessageBody.fontSize.value)
            }
        },
        update = { textView ->
            textView.text = ChatAssistantMarkdownRenderer.spanned(text, palette)
        },
    )
}
