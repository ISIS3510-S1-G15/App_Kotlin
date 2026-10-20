package com.uniandes.campuseats.analytics

class FakeTransport(private val failure: Exception? = null) : AnalyticsTransport {
    val requests = mutableListOf<Pair<String, Map<String, Any>>>()

    override suspend fun post(path: String, body: Map<String, Any>) {
        requests.add(path to body)
        failure?.let { throw it }
    }
}
