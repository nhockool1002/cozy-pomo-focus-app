package com.cozypomo.app.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Endpoint hiện có ở backend/src/auth. Mở rộng dần theo mục 4.4 docs/technical-spec.md
 * (sessions, eggs, collection, currency, shop, stats, settings, sync).
 */
interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPairResponse

    @Headers("Requires-Auth: true")
    @GET("auth/me")
    suspend fun me(): UserDto
}
