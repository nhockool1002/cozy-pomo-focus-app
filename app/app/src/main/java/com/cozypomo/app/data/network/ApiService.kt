package com.cozypomo.app.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoint hiện có ở backend/src/auth, src/sessions, src/eggs, src/currency.
 * Mở rộng dần theo mục 4.4 docs/technical-spec.md (collection, shop, stats, settings, sync).
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

    @Headers("Requires-Auth: true")
    @POST("sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionDto

    @Headers("Requires-Auth: true")
    @PATCH("sessions/{id}/complete")
    suspend fun completeSession(@Path("id") id: String, @Body body: CompleteSessionRequest): CompleteSessionResponse

    @Headers("Requires-Auth: true")
    @PATCH("sessions/{id}/give-up")
    suspend fun giveUpSession(@Path("id") id: String): SessionDto

    @GET("egg-types")
    suspend fun getEggTypes(): List<EggTypeDto>

    @Headers("Requires-Auth: true")
    @GET("currency/balance")
    suspend fun getBalance(): BalanceResponse
}
