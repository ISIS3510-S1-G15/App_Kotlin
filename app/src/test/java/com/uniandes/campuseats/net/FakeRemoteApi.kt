package com.uniandes.campuseats.net

import org.json.JSONObject

class FakeRemoteApi(
    private val responses: MutableMap<String, JSONObject> = mutableMapOf(),
    private val failure: Exception? = null
) : RemoteApi {
    val calls = mutableListOf<Triple<String, String, Map<String, Any>?>>()

    fun respond(path: String, json: String) {
        responses[path] = JSONObject(json)
    }

    private fun handle(method: String, path: String, body: Map<String, Any>?): JSONObject {
        calls.add(Triple(method, path, body))
        failure?.let { throw it }
        return responses[path] ?: JSONObject()
    }

    override suspend fun get(path: String) = handle("GET", path, null)
    override suspend fun post(path: String, body: Map<String, Any>) = handle("POST", path, body)
    override suspend fun put(path: String, body: Map<String, Any>?) = handle("PUT", path, body)
    override suspend fun delete(path: String) = handle("DELETE", path, null)
}
