package com.uniandes.campuseats.auth

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUserTest {

    private fun json(admin: Boolean?) = JSONObject()
        .put("id", 1)
        .put("name", "Ana")
        .put("email", "ana@uniandes.edu.co")
        .also { if (admin != null) it.put("is_analytics_admin", admin) }

    @Test
    fun missingFlagMeansNotAdmin() {
        assertFalse(AuthUser.fromJson(json(null)).isAnalyticsAdmin)
    }

    @Test
    fun flagIsParsed() {
        assertTrue(AuthUser.fromJson(json(true)).isAnalyticsAdmin)
        assertFalse(AuthUser.fromJson(json(false)).isAnalyticsAdmin)
    }

    @Test
    fun flagSurvivesSerialization() {
        val user = AuthUser(1, "Ana", "ana@uniandes.edu.co", isAnalyticsAdmin = true)
        assertTrue(AuthUser.fromJson(user.toJson()).isAnalyticsAdmin)
    }
}
