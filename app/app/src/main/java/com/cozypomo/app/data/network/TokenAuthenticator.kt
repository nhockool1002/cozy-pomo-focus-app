package com.cozypomo.app.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.cozypomo.app.data.auth.AuthPreferencesKeys
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tự làm mới access token khi gặp 401 (JWT hết hạn sau 15 phút — nguồn gốc lỗi 401 lặp lại nhiều
 * lần khi tự kiểm thử, xem T-041/T-081 "Chưa làm"). Dùng OkHttp [Authenticator] (không phải
 * Interceptor) vì đây là cơ chế duy nhất OkHttp gọi lại kèm request gốc để retry sau 401.
 *
 * [refreshApi] trỏ tới 1 Retrofit client RIÊNG không gắn chính Authenticator này (xem
 * NetworkModule) — nếu dùng chung client với [ApiService] sẽ đệ quy vô hạn khi bản thân
 * /auth/refresh cũng trả 401.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val dataStore: DataStore<Preferences>,
    private val refreshApi: RefreshApiService,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Request không gắn JWT thì 401 không phải do token hết hạn — không phải việc của mình.
        val failedAuthHeader = response.request.header("Authorization") ?: return null
        if (responseCount(response) >= 2) return null // đã thử refresh 1 lần rồi vẫn 401 — dừng, tránh lặp vô hạn.

        synchronized(this) {
            val failedToken = failedAuthHeader.removePrefix("Bearer ")
            val currentToken = tokenProvider.accessToken
            // Luồng khác (request song song) đã refresh xong trong lúc request này chờ lock —
            // dùng token mới có sẵn, không cần gọi /auth/refresh thêm lần nữa.
            if (currentToken != null && currentToken != failedToken) {
                return response.request.newBuilder().header("Authorization", "Bearer $currentToken").build()
            }

            val refreshToken = tokenProvider.refreshToken ?: return null
            val newTokens = runBlocking {
                runCatching { refreshApi.refresh(RefreshRequest(refreshToken)) }.getOrNull()
            }

            if (newTokens == null) {
                // Refresh token cũng hết hạn/không hợp lệ (VD >30 ngày không mở app) — xoá phiên.
                // AuthRepository.isLoggedIn đọc thẳng từ DataStore nên tự chuyển false ngay, nơi
                // quan sát Flow này (CozyPomoNavHost) tự điều hướng về Login — xem RootNavHost.
                tokenProvider.clear()
                runBlocking {
                    dataStore.edit {
                        it.remove(AuthPreferencesKeys.ACCESS_TOKEN)
                        it.remove(AuthPreferencesKeys.REFRESH_TOKEN)
                        it.remove(AuthPreferencesKeys.ACCOUNT_EMAIL)
                        it.remove(AuthPreferencesKeys.ACCOUNT_ID)
                    }
                }
                return null
            }

            tokenProvider.accessToken = newTokens.accessToken
            tokenProvider.refreshToken = newTokens.refreshToken
            runBlocking {
                dataStore.edit {
                    it[AuthPreferencesKeys.ACCESS_TOKEN] = newTokens.accessToken
                    it[AuthPreferencesKeys.REFRESH_TOKEN] = newTokens.refreshToken
                }
            }
            return response.request.newBuilder().header("Authorization", "Bearer ${newTokens.accessToken}").build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
