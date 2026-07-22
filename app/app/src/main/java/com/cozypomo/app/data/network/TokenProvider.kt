package com.cozypomo.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Giữ access/refresh token trong bộ nhớ cho scaffold ban đầu.
 * TODO: chuyển sang lưu bền bằng Jetpack DataStore trước khi triển khai đăng nhập thật.
 */
@Singleton
class TokenProvider @Inject constructor() {
    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null

    fun clear() {
        accessToken = null
        refreshToken = null
    }
}
