package com.uniandes.campuseats.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validCredentialsHaveNoProblem() {
        assertNull(AuthValidation.validate(null, "ana@uniandes.edu.co", "password1"))
        assertNull(AuthValidation.validate("Ana", " Ana@Uniandes.edu.co ", "password1"))
    }

    @Test
    fun nonInstitutionalEmailIsRejected() {
        assertEquals(
            "Use your institutional email (@uniandes.edu.co)",
            AuthValidation.validate(null, "ana@gmail.com", "password1")
        )
        assertEquals(
            "Use your institutional email (@uniandes.edu.co)",
            AuthValidation.validate(null, "@uniandes.edu.co", "password1")
        )
        assertEquals(
            "Use your institutional email (@uniandes.edu.co)",
            AuthValidation.validate(null, "a@b@uniandes.edu.co", "password1")
        )
    }

    @Test
    fun shortPasswordIsRejected() {
        assertEquals(
            "Password must have at least 8 characters",
            AuthValidation.validate(null, "ana@uniandes.edu.co", "short")
        )
    }

    @Test
    fun blankNameIsRejectedOnlyWhenNameIsRequired() {
        assertEquals("Enter your name", AuthValidation.validate("  ", "ana@uniandes.edu.co", "password1"))
        assertNull(AuthValidation.validate(null, "ana@uniandes.edu.co", "password1"))
    }
}
