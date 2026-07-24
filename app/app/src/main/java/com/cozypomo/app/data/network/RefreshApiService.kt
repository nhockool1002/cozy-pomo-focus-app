package com.cozypomo.app.data.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Chỉ 1 endpoint /auth/refresh, dùng bởi [TokenAuthenticator] — tách riêng khỏi [ApiService]
 * vì client build endpoint này KHÔNG được gắn [TokenAuthenticator] (client chính có gắn), nếu
 * không sẽ đệ quy vô hạn khi chính /auth/refresh cũng trả 401.
 */
interface RefreshApiService {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPairResponse
}
