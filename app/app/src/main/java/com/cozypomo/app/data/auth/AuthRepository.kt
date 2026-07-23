package com.cozypomo.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.AuthResponse
import com.cozypomo.app.data.network.LoginRequest
import com.cozypomo.app.data.network.RegisterRequest
import com.cozypomo.app.data.network.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val apiService: ApiService,
    private val tokenProvider: TokenProvider,
) {
    val hasSeenOnboarding: Flow<Boolean> =
        dataStore.data.map { it[AuthPreferencesKeys.ONBOARDING_SEEN] ?: false }

    val isLoggedIn: Flow<Boolean> =
        dataStore.data.map { !it[AuthPreferencesKeys.ACCESS_TOKEN].isNullOrBlank() }

    /** Cache tại lúc đăng nhập/đăng ký — tránh phải gọi GET /auth/me (dễ 401 vì JWT hết hạn 15 phút, chưa có refresh flow) chỉ để hiện email ở màn Cài đặt. */
    val accountEmail: Flow<String?> =
        dataStore.data.map { it[AuthPreferencesKeys.ACCOUNT_EMAIL] }

    val accountId: Flow<String?> =
        dataStore.data.map { it[AuthPreferencesKeys.ACCOUNT_ID] }

    suspend fun restoreSession() {
        val prefs = dataStore.data.first()
        tokenProvider.accessToken = prefs[AuthPreferencesKeys.ACCESS_TOKEN]
        tokenProvider.refreshToken = prefs[AuthPreferencesKeys.REFRESH_TOKEN]
    }

    suspend fun completeOnboarding() {
        dataStore.edit { it[AuthPreferencesKeys.ONBOARDING_SEEN] = true }
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = apiService.login(LoginRequest(email = email, password = password))
        persistSession(response)
    }

    suspend fun register(email: String, password: String, displayName: String?): Result<Unit> = runCatching {
        val response = apiService.register(
            RegisterRequest(email = email, password = password, displayName = displayName?.ifBlank { null }),
        )
        persistSession(response)
    }

    suspend fun logout() {
        tokenProvider.clear()
        dataStore.edit {
            it.remove(AuthPreferencesKeys.ACCESS_TOKEN)
            it.remove(AuthPreferencesKeys.REFRESH_TOKEN)
            it.remove(AuthPreferencesKeys.ACCOUNT_EMAIL)
            it.remove(AuthPreferencesKeys.ACCOUNT_ID)
        }
    }

    private suspend fun persistSession(response: AuthResponse) {
        tokenProvider.accessToken = response.accessToken
        tokenProvider.refreshToken = response.refreshToken
        dataStore.edit {
            it[AuthPreferencesKeys.ACCESS_TOKEN] = response.accessToken
            it[AuthPreferencesKeys.REFRESH_TOKEN] = response.refreshToken
            it[AuthPreferencesKeys.ACCOUNT_EMAIL] = response.user.email
            it[AuthPreferencesKeys.ACCOUNT_ID] = response.user.id
        }
    }
}
