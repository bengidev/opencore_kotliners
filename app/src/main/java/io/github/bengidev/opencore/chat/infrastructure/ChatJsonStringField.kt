package io.github.bengidev.opencore.chat.infrastructure

internal object ChatJsonStringField {
    fun extract(json: String, key: String): String? {
        val keyToken = "\"$key\""
        var searchFrom = 0
        while (true) {
            val keyIndex = json.indexOf(keyToken, searchFrom)
            if (keyIndex < 0) return null
            var index = keyIndex + keyToken.length
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != ':') {
                searchFrom = keyIndex + 1
                continue
            }
            index++
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != '"') {
                searchFrom = keyIndex + 1
                continue
            }
            index++
            val output = StringBuilder()
            while (index < json.length) {
                when (val char = json[index]) {
                    '"' -> return output.toString()
                    '\\' -> {
                        index++
                        if (index >= json.length) return null
                        output.append(
                            when (val escaped = json[index]) {
                                '"', '\\', '/' -> escaped
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'u' -> {
                                    if (index + 4 >= json.length) return null
                                    val codePoint = json.substring(index + 1, index + 5).toInt(16)
                                    index += 4
                                    codePoint.toChar()
                                }
                                else -> escaped
                            }
                        )
                    }
                    else -> output.append(char)
                }
                index++
            }
            return null
        }
    }

    fun appendQuoted(builder: StringBuilder, value: String) {
        builder.append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }
        builder.append('"')
    }
}
