package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import java.math.BigDecimal

/** Parses OpenAI-compatible model catalog entries into domain [SidePanelModel] values. */
internal object ProviderCatalogParser {
    fun parse(responseBody: String): List<SidePanelModel> {
        val dataStart = responseBody.indexOf("\"data\"")
        if (dataStart < 0) return emptyList()
        val arrayStart = responseBody.indexOf('[', dataStart)
        if (arrayStart < 0) return emptyList()

        return extractObjects(responseBody, arrayStart + 1)
            .mapNotNull(::parseEntryInternal)
            .filter { entry ->
                val modality = entry.modality
                modality.isNullOrEmpty() || modality.contains("text", ignoreCase = true)
            }
            .map { it.toSidePanelModel() }
            .sortedWith(compareByDescending<SidePanelModel> { it.isFree }.thenBy { it.displayTitle })
    }

    fun parseEntry(objectJson: String): SidePanelModel =
        requireNotNull(parseEntryInternal(objectJson)).toSidePanelModel()

    private data class ParsedEntry(
        val id: String,
        val name: String?,
        val contextLength: Int?,
        val modality: String?,
        val tokenizer: String?,
        val promptPrice: String?,
        val completionPrice: String?,
        val supportedParameters: List<String>,
        val supportedReasoningEfforts: List<String>,
        val reasoningMandatory: Boolean
    ) {
        val isFree: Boolean
            get() = when {
                id.endsWith(":free", ignoreCase = true) -> true
                promptPrice != null && completionPrice != null ->
                    isZeroPrice(promptPrice) && isZeroPrice(completionPrice)
                name != null -> name.contains("free", ignoreCase = true)
                else -> false
            }

        val supportsSpeedModes: Boolean
            get() = tokenizer == "Router" ||
                supportedParameters.any { it.equals("provider", ignoreCase = true) } ||
                id.equals("openrouter/free", ignoreCase = true)

        fun toSidePanelModel(): SidePanelModel = SidePanelModel(
            id = id,
            displayTitle = name ?: id,
            isFree = isFree,
            contextLength = contextLength,
            supportedReasoningEfforts = supportedReasoningEfforts,
            reasoningMandatory = reasoningMandatory,
            supportsSpeedModes = supportsSpeedModes,
            supportsImageInput = modalitySupportsImage(modality),
            supportsVideoInput = modalitySupportsVideo(modality),
        )
    }

    private fun parseEntryInternal(objectJson: String): ParsedEntry? {
        val id = ChatJsonStringField.extract(objectJson, "id") ?: return null
        val pricingObject = extractNestedObject(objectJson, "pricing")
        val architectureObject = extractNestedObject(objectJson, "architecture")
        val reasoningObject = extractNestedObject(objectJson, "reasoning")
        val supportedParameters = extractStringArray(objectJson, "supported_parameters")
        val modality = architectureObject?.let { ChatJsonStringField.extract(it, "modality") }
        val supportedEfforts = reasoningObject
            ?.let { extractStringArray(it, "supported_efforts") }
            .orEmpty()
        val reasoningMandatory = reasoningObject?.let { parseBooleanField(it, "mandatory") } == true
        val resolvedEfforts = when {
            supportedEfforts.isNotEmpty() -> supportedEfforts
            supportsReasoningParameter(id, modality, supportedParameters) -> DEFAULT_GATEWAY_REASONING_EFFORTS
            else -> emptyList()
        }
        return ParsedEntry(
            id = id,
            name = ChatJsonStringField.extract(objectJson, "name"),
            contextLength = resolveContextLength(objectJson),
            modality = modality,
            tokenizer = architectureObject?.let { ChatJsonStringField.extract(it, "tokenizer") },
            promptPrice = pricingObject?.let { ChatJsonStringField.extract(it, "prompt") },
            completionPrice = pricingObject?.let { ChatJsonStringField.extract(it, "completion") },
            supportedParameters = supportedParameters,
            supportedReasoningEfforts = resolvedEfforts,
            reasoningMandatory = reasoningMandatory
        )
    }

    private fun supportsReasoningParameter(
        id: String,
        modality: String?,
        supportedParameters: List<String>
    ): Boolean {
        if (modality?.contains("reasoning", ignoreCase = true) == true) return true
        if (supportedParameters.any { it == "reasoning" || it == "reasoning_effort" }) return true
        if (id.contains(":thinking", ignoreCase = true)) return true
        if (id.endsWith("-thinking", ignoreCase = true)) return true
        return false
    }

    private fun resolveContextLength(objectJson: String): Int? =
        extractIntField(objectJson, "context_length")
            ?: extractIntField(objectJson, "context")
            ?: extractIntField(objectJson, "max_model_len")

    private fun extractObjects(json: String, startIndex: Int): List<String> {
        val objects = mutableListOf<String>()
        var index = startIndex
        while (index < json.length) {
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] == ']') break
            if (json[index] != '{') {
                index++
                continue
            }
            val end = findMatchingBrace(json, index) ?: break
            objects += json.substring(index, end + 1)
            index = end + 1
            while (index < json.length && json[index].isWhitespace()) index++
            if (index < json.length && json[index] == ',') index++
        }
        return objects
    }

    private fun extractNestedObject(json: String, key: String): String? {
        val keyPattern = "\"$key\"\\s*:".toRegex()
        val match = keyPattern.find(json) ?: return null
        var index = match.range.last + 1
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '{') return null
        val end = findMatchingBrace(json, index) ?: return null
        return json.substring(index, end + 1)
    }

    private fun parseBooleanField(json: String, key: String): Boolean? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        return when {
            json.startsWith("true", index) -> true
            json.startsWith("false", index) -> false
            else -> null
        }
    }

    private fun extractIntField(json: String, key: String): Int? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        val start = index
        while (index < json.length && (json[index].isDigit() || json[index] == '-')) index++
        if (start == index) return null
        return json.substring(start, index).toIntOrNull()
    }

    private fun extractStringArray(json: String, key: String): List<String> {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return emptyList()
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return emptyList()
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '[') return emptyList()
        index++
        val values = mutableListOf<String>()
        while (index < json.length) {
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] == ']') break
            if (json[index] != '"') {
                index++
                continue
            }
            index++
            val start = index
            while (index < json.length && json[index] != '"') {
                if (json[index] == '\\') index++
                index++
            }
            if (start < index) values += json.substring(start, index)
            index++
            while (index < json.length && json[index].isWhitespace()) index++
            if (index < json.length && json[index] == ',') index++
        }
        return values
    }

    private fun isZeroPrice(value: String): Boolean =
        value.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) == 0 || value == "0"

    private fun modalitySupportsImage(modality: String?): Boolean =
        modality?.contains("image", ignoreCase = true) == true

    private fun modalitySupportsVideo(modality: String?): Boolean =
        modality?.contains("video", ignoreCase = true) == true

    private fun findMatchingBrace(json: String, openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private val DEFAULT_GATEWAY_REASONING_EFFORTS = listOf(
        "max", "xhigh", "high", "medium", "low", "minimal", "none"
    )
}
