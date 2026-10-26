package com.uniandes.campuseats.auth

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

interface SessionStorage {
    var token: String?
    var user: AuthUser?
    fun clear()
}

class SessionStore(context: Context) : SessionStorage {

    private val prefs = context.applicationContext.getSharedPreferences("campus_eats_session", Context.MODE_PRIVATE)

    override var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    override var user: AuthUser?
        get() = prefs.getString(KEY_USER, null)?.let { runCatching { AuthUser.fromJson(JSONObject(it)) }.getOrNull() }
        set(value) = prefs.edit { putString(KEY_USER, value?.toJson()?.toString()) }

    override fun clear() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_USER)
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}
