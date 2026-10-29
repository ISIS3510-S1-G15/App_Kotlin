package com.uniandes.campuseats.data.local

import org.json.JSONArray

object StringListCodec {

    fun encode(values: List<String>): String = JSONArray(values).toString()

    fun decode(raw: String): List<String> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}
