package com.uniandes.campuseats.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

interface SessionStorage {
    var token: String?
    var user: AuthUser?
    fun clear()
}

class SessionStore(context: Context) : SessionStorage {

    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "campus_eats_session_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Se obliga a las sesiones que esten antes de la migracion a cerrarse 
        context.applicationContext.getSharedPreferences("campus_eats_session", Context.MODE_PRIVATE)
            .edit { clear() }
    }

    override var token: String?
        get() = encryptedPrefs.getString(KEY_TOKEN, null)
        set(value) = encryptedPrefs.edit { putString(KEY_TOKEN, value) }

    override var user: AuthUser?
        get() = encryptedPrefs.getString(KEY_USER, null)?.let { runCatching { AuthUser.fromJson(JSONObject(it)) }.getOrNull() }
        set(value) = encryptedPrefs.edit { putString(KEY_USER, value?.toJson()?.toString()) }

    override fun clear() {
        encryptedPrefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_USER)
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}