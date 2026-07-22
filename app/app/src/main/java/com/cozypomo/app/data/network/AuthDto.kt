package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val authProvider: String,
    val displayName: String? = null,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
)
