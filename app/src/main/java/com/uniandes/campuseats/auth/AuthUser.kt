package com.uniandes.campuseats.auth

import org.json.JSONObject

data class AuthUser(val id: Int, val name: String, val email: String, val isAnalyticsAdmin: Boolean = false) {
    fun toJson(): JSONObject =
        JSONObject().put("id", id).put("name", name).put("email", email).put("is_analytics_admin", isAnalyticsAdmin)

    companion object {
        fun fromJson(json: JSONObject) = AuthUser(
            id = json.getInt("id"),
            name = json.getString("name"),
            email = json.getString("email"),
            isAnalyticsAdmin = json.optBoolean("is_analytics_admin", false)
        )
    }
}

class AuthException(message: String) : Exception(message)
