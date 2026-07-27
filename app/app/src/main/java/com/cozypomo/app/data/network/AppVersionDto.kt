package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

/** T-121 — Force Update: `GET /app-version` không cần JWT, gọi được ngay ở Splash (T-122). */
@Serializable
data class AppVersionDto(
    val id: Int = 1,
    val latestVersionCode: Int = 1,
    val minSupportedVersionCode: Int = 1,
    val updateUrl: String = "https://play.google.com/store/apps/details?id=com.cozypomo.app",
    val updateTitle: String = "Cần cập nhật phiên bản mới",
    val updateMessage: String = "Phiên bản bạn đang dùng đã quá cũ, vui lòng cập nhật để tiếp tục sử dụng CozyPomo.",
)
