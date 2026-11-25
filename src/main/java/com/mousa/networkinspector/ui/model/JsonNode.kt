package com.mousa.networkinspector.ui.model

sealed class JsonNode {
    data class ObjectNode(val entries: Map<String, JsonNode>) : JsonNode()
    data class ArrayNode(val items: List<JsonNode>) : JsonNode()
    data class ValueNode(val value: String) : JsonNode()
}
