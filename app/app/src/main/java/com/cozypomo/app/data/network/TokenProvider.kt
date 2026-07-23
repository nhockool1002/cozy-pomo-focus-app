package com.cozypomo.app.data.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache token trong RAM để AuthInterceptor đọc đồng bộ trên mỗi request.
 * Nguồn bền (DataStore) và vòng đời đăng nhập/đăng xuất do AuthRepository quản lý —
 * xem AuthRepository.restoreSession/login/register/logout.
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
