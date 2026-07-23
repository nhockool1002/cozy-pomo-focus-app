package com.cozypomo.app.data.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @Headers("Requires-Auth: true")
    @GET("owned-eggs")
    suspend fun getOwnedEggs(@Query("status") status: String? = null): List<OwnedEggDto>

    @GET("game-settings")
    suspend fun getGameSettings(): GameSettingsDto

    @GET("species")
    suspend fun getSpecies(@Query("category") category: String? = null, @Query("rarity") rarity: String? = null): List<SpeciesDto>

    @Headers("Requires-Auth: true")
    @GET("collection")
    suspend fun getCollection(@Query("category") category: String? = null, @Query("rarity") rarity: String? = null): List<CollectionEntryDto>

    @Headers("Requires-Auth: true")
    @GET("collection/progress")
    suspend fun getCollectionProgress(): CollectionProgressDto

    @Headers("Requires-Auth: true")
    @PATCH("collection/{speciesId}/favorite")
    suspend fun toggleFavorite(@Path("speciesId") speciesId: String): FavoriteToggleResponseDto

    @GET("shop-items")
    suspend fun getShopItems(@Query("category") category: String? = null): List<ShopItemDto>

    @Headers("Requires-Auth: true")
    @GET("inventory")
    suspend fun getInventory(): List<InventoryItemDto>

    @Headers("Requires-Auth: true")
    @PATCH("inventory/{id}/equip")
    suspend fun toggleEquip(@Path("id") id: String): InventoryItemDto

    /**
     * Trả về [ResponseBody] thô thay vì kiểu cụ thể — response thật ra khác nhau tuỳ danh mục
     * (OwnedEgg cho EGG, InventoryItem cho JAR_SKIN/MUSIC) nên không có 1 kiểu chung để parse an
     * toàn; chỉ cần biết thành công hay không rồi gọi lại getOwnedEggs/getInventory/getBalance.
     */
    @Headers("Requires-Auth: true")
    @POST("shop-items/{id}/purchase")
    suspend fun purchaseShopItem(@Path("id") id: String, @Body body: PurchaseRequest): ResponseBody

    // --- Debug/cheat (chỉ tài khoản tester — server tự chặn nếu không phải, xem DebugController) ---

    @Headers("Requires-Auth: true")
    @POST("debug/grant-currency")
    suspend fun debugGrantCurrency(@Body body: GrantCurrencyRequest): BalanceResponse

    @Headers("Requires-Auth: true")
    @POST("debug/grant-egg")
    suspend fun debugGrantEgg(@Body body: GrantEggRequest): OwnedEggDto

    @Headers("Requires-Auth: true")
    @POST("debug/grant-species")
    suspend fun debugGrantSpecies(@Body body: GrantSpeciesRequest): CollectionEntryDto
}
