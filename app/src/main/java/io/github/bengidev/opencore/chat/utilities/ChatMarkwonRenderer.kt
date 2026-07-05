package io.github.bengidev.opencore.chat.utilities

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.util.Linkify
import android.widget.TextView
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.Code

internal object ChatMarkwonRenderer {
    enum class Profile {
        Assistant,
        Thinking,
    }

    private val cache = BoundedSpannedCache()

    fun spanned(
        markdown: String,
        palette: OpenCorePalette,
        profile: Profile,
        context: Context? = null,
    ): Spanned {
        val normalized = ChatAssistantMarkdownPreprocessor.normalize(markdown)
        val cacheKey = cacheKey(normalized, profile)
        cache.get(cacheKey, palette.isDark)?.let { return it }

        val rendered = render(
            markdown = normalized,
            palette = palette,
            profile = profile,
            context = requireContext(context),
        )
        cache.put(cacheKey, palette.isDark, rendered)
        return rendered
    }

    /**
     * Applies cached markdown to [textView] via Markwon's [Markwon.setParsedMarkdown] so table
     * plugins can schedule [io.noties.markwon.ext.tables.TableRowSpan] layout.
     */
    fun applyTo(
        textView: TextView,
        markdown: String,
        palette: OpenCorePalette,
        profile: Profile,
        context: Context,
    ) {
        val normalized = ChatAssistantMarkdownPreprocessor.normalize(markdown)
        val cacheKey = cacheKey(normalized, profile)
        val markwon = createMarkwon(context, palette, profile)
        val rendered = cache.get(cacheKey, palette.isDark)
            ?: render(normalized, palette, profile, context).also {
                cache.put(cacheKey, palette.isDark, it)
            }
        markwon.setParsedMarkdown(textView, rendered)
    }

    private fun render(
        markdown: String,
        palette: OpenCorePalette,
        profile: Profile,
        context: Context,
    ): Spanned {
        val markwon = createMarkwon(context, palette, profile)
        val rendered = SpannableStringBuilder(markwon.toMarkdown(markdown))
        applyAutoLinks(rendered)
        if (profile == Profile.Thinking) {
            applyThinkingBodyStyle(rendered)
        }
        return rendered
    }

    private fun createMarkwon(
        context: Context,
        palette: OpenCorePalette,
        profile: Profile,
    ): Markwon =
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(
                object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        ChatMarkwonTheme.apply(builder, palette, profile)
                    }

                    override fun configureSpansFactory(builder: io.noties.markwon.MarkwonSpansFactory.Builder) {
                        builder.appendFactory(Code::class.java) { _, _ ->
                            TypefaceSpan("monospace")
                        }
                    }
                },
            )
            .build()

    private fun applyAutoLinks(spannable: SpannableStringBuilder) {
        Linkify.addLinks(spannable, Linkify.WEB_URLS)
    }

    private fun applyThinkingBodyStyle(spannable: Spannable) {
        val end = spannable.length
        if (end == 0) return

        spannable.setSpan(
            TypefaceSpan("monospace"),
            0,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            StyleSpan(Typeface.ITALIC),
            0,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    private fun cacheKey(normalized: String, profile: Profile): String =
        "${profile.name}\u0000$normalized"

    private fun requireContext(context: Context?): Context =
        checkNotNull(context) { "Context is required for Markwon rendering." }
}
