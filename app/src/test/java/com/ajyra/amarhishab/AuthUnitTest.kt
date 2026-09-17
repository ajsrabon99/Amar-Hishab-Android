package com.ajyra.amarhishab

import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.AuthResponseDto
import com.ajyra.amarhishab.network.LoginRequestDto
import com.ajyra.amarhishab.network.RegisterRequestDto
import com.ajyra.amarhishab.network.ResendCodeRequestDto
import com.ajyra.amarhishab.network.UserDto
import com.ajyra.amarhishab.network.VerifyCodeRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUnitTest {

    @Test
    fun testLoginRequestCreation() {
        val req = LoginRequestDto(
            identifier = "testuser",
            password = "testpassword123"
        )
        assertEquals("testuser", req.identifier)
        assertEquals("testpassword123", req.password)
    }

    @Test
    fun testRegisterRequestCreation() {
        val req = RegisterRequestDto(
            username = "srabon",
            email = "srabon@example.com",
            password = "SecurePassword123!",
            confirmPassword = "SecurePassword123!"
        )
        assertEquals("srabon", req.username)
        assertEquals("srabon@example.com", req.email)
        assertEquals("SecurePassword123!", req.password)
    }

    @Test
    fun testVerifyCodeRequestCreation() {
        val req = VerifyCodeRequestDto(
            identifier = "srabon@example.com",
            code = "123456"
        )
        assertEquals("srabon@example.com", req.identifier)
        assertEquals("123456", req.code)
    }

    @Test
    fun testResendCodeRequestCreation() {
        val req = ResendCodeRequestDto(
            identifier = "srabon@example.com"
        )
        assertEquals("srabon@example.com", req.identifier)
    }

    @Test
    fun testUserModelMapping() {
        val userDto = UserDto(
            id = "user-101",
            email = "user@amarhishab.app",
            username = "hishabi",
            firstName = "Srabon",
            lastName = "Khan",
            isVerified = true
        )
        val user = User(
            id = userDto.id ?: "",
            email = userDto.email ?: "",
            username = userDto.username ?: "",
            firstName = userDto.firstName.orEmpty(),
            lastName = userDto.lastName.orEmpty(),
            displayName = "Srabon Khan",
            isVerified = userDto.isVerified ?: true
        )
        assertEquals("Srabon Khan", user.name)
        assertTrue(user.isVerified)
    }

    @Test
    fun testAuthResponseParsing() {
        val response = AuthResponseDto(
            status = "success",
            token = "ah_token_9876543210abcdef",
            user = UserDto(
                id = "1",
                email = "admin@amarhishab.app",
                username = "admin"
            ),
            message = "Authentication successful"
        )
        assertEquals("success", response.status)
        assertNotNull(response.token)
        assertEquals("admin", response.user?.username)
    }
}
