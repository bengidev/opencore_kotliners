package io.github.bengidev.opencore.chat.utilities

import android.text.Spanned
import android.util.LruCache

internal class BoundedSpannedCache(limit: Int = 64) {
    private data class Key(val content: String, val isDark: Boolean)

    private val cache = LruCache<Key, Spanned>(limit)

    fun get(content: String, isDark: Boolean): Spanned? = cache.get(Key(content, isDark))

    fun put(content: String, isDark: Boolean, value: Spanned) {
        cache.put(Key(content, isDark), value)
    }
}
