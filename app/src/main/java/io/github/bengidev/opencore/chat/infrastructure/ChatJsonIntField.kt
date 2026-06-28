package io.github.bengidev.opencore.chat.infrastructure

internal object ChatJsonIntField {
    fun extract(json: String, key: String): Int? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length) return null
        val start = index
        if (json[index] == '-') index++
        while (index < json.length && json[index].isDigit()) index++
        if (index == start || (index == start + 1 && json[start] == '-')) return null
        return json.substring(start, index).toIntOrNull()
    }
}
