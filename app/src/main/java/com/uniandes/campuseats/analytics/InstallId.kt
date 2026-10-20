package com.uniandes.campuseats.analytics

import android.content.Context
import java.util.UUID

object InstallId {
    private const val PREFS = "campus_eats_analytics"
    private const val KEY = "install_id"

    fun get(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY, it).apply()
        }
    }
}
