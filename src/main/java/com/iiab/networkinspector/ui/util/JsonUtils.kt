package com.iiab.networkinspector.ui.util

import com.iiab.networkinspector.ui.model.JsonNode
import org.json.JSONArray
import org.json.JSONObject

fun tryParseJsonToTree(raw: String): JsonNode? {
    if (raw == "—" || raw.isBlank()) return null
    return try {
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("{") -> parseJsonObject(JSONObject(trimmed))
            trimmed.startsWith("[") -> parseJsonArray(JSONArray(trimmed))
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun parseJsonObject(obj: JSONObject): JsonNode.ObjectNode {
    val entries = mutableMapOf<String, JsonNode>()
    obj.keys().forEach { key ->
        val value = obj.get(key)
        entries[key] = when (value) {
            is JSONObject -> parseJsonObject(value)
            is JSONArray -> parseJsonArray(value)
            else -> JsonNode.ValueNode(value.toString())
        }
    }
    return JsonNode.ObjectNode(entries)
}

fun parseJsonArray(arr: JSONArray): JsonNode.ArrayNode {
    val items = (0 until arr.length()).map { i ->
        val value = arr.get(i)
        when (value) {
            is JSONObject -> parseJsonObject(value)
            is JSONArray -> parseJsonArray(value)
            else -> JsonNode.ValueNode(value.toString())
        }
    }
    return JsonNode.ArrayNode(items)
}

fun tryFormatJson(raw: String): String {
    if (raw == "—" || raw.isBlank()) return raw
    return try {
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: Exception) {
        raw
    }
}

fun flattenJsonNode(node: JsonNode): String {
    return when (node) {
        is JsonNode.ValueNode -> node.value
        is JsonNode.ObjectNode -> {
            buildString {
                append("{")
                append(
                    node.entries.entries.joinToString(", ") { (k, v) ->
                        "\"$k\": ${flattenJsonNode(v)}"
                    }
                )
                append("}")
            }
        }
        is JsonNode.ArrayNode -> {
            buildString {
                append("[")
                append(node.items.joinToString(", ") { flattenJsonNode(it) })
                append("]")
            }
        }
    }
}

fun parseJsonOrString(body: String?): Any {
    if (body == null) return JSONObject.NULL
    val trimmed = body.trim()
    if (trimmed.isEmpty()) return JSONObject.NULL

    return try {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed)
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> body
        }
    } catch (_: Exception) {
        body
    }
}
