package com.uniandes.campuseats.auth

object AuthValidation {
    const val INSTITUTIONAL_DOMAIN = "@uniandes.edu.co"
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 72

    fun validate(name: String?, email: String, password: String): String? {
        val cleanEmail = email.trim().lowercase()
        return when {
            name != null && name.isBlank() -> "Enter your name"
            !isInstitutionalEmail(cleanEmail) -> "Use your institutional email ($INSTITUTIONAL_DOMAIN)"
            password.length < MIN_PASSWORD_LENGTH -> "Password must have at least $MIN_PASSWORD_LENGTH characters"
            password.length > MAX_PASSWORD_LENGTH -> "Password must have at most $MAX_PASSWORD_LENGTH characters"
            name != null && !isStrongPassword(password) ->
                "Password needs at least one letter, one number and one special character"
            else -> null
        }
    }

    fun isStrongPassword(password: String): Boolean =
        password.any { it in 'A'..'Z' || it in 'a'..'z' } &&
            password.any { it in '0'..'9' } &&
            password.any { !(it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it.isWhitespace()) }

    fun isInstitutionalEmail(email: String): Boolean =
        email.endsWith(INSTITUTIONAL_DOMAIN) && email.count { it == '@' } == 1 && !email.startsWith("@")
}
