package com.uniandes.campuseats.analytics

import com.uniandes.campuseats.net.HttpRemoteApi
import com.uniandes.campuseats.net.RemoteApi

interface AnalyticsTransport {
    suspend fun post(path: String, body: Map<String, Any>)
}

class HttpAnalyticsTransport(private val api: RemoteApi) : AnalyticsTransport {

    constructor(baseUrl: String, timeoutMs: Int = 5_000) : this(HttpRemoteApi(baseUrl, timeoutMs))

    override suspend fun post(path: String, body: Map<String, Any>) {
        api.post(path, body)
    }
}
