package com.uniandes.campuseats.auth

import org.json.JSONObject

data class AuthUser(val id: Int, val name: String, val email: String) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("email", email)

    companion object {
        fun fromJson(json: JSONObject) = AuthUser(
            id = json.getInt("id"),
            name = json.getString("name"),
            email = json.getString("email")
        )
    }
}

class AuthException(message: String) : Exception(message)
