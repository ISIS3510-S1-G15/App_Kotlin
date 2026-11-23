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

    private val encryptedPrefs = openEncryptedPrefs(context.applicationContext)

    /**
     * Si el archivo cifrado no se puede abrir (clave del Keystore perdida tras un restore o cambio de dispositivo)
     * se borra y se recrea: el usuario solo tiene que iniciar sesion otra vez en vez de ver un crash al arrancar.
     */
    private fun openEncryptedPrefs(app: Context) = try {
        createEncryptedPrefs(app)
    } catch (e: Exception) {
        app.deleteSharedPreferences(FILE_NAME)
        createEncryptedPrefs(app)
    }

    private fun createEncryptedPrefs(app: Context) = EncryptedSharedPreferences.create(
        app,
        FILE_NAME,
        MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
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
        const val FILE_NAME = "campus_eats_session_secure"
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}