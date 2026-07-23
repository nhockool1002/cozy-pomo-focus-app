package com.cozypomo.app.data.auth

import retrofit2.HttpException
import java.io.IOException

/** Dịch lỗi network/HTTP thô thành thông báo tiếng Việt dễ hiểu cho màn Đăng nhập/Đăng ký. */
fun Throwable.toAuthErrorMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Sai email hoặc mật khẩu."
        409 -> "Email này đã được sử dụng."
        400, 422 -> "Thông tin chưa hợp lệ, vui lòng kiểm tra lại."
        else -> "Máy chủ đang gặp sự cố (mã ${code()}). Thử lại sau."
    }
    is IOException -> "Không thể kết nối máy chủ. Kiểm tra mạng và thử lại."
    else -> "Có lỗi xảy ra, vui lòng thử lại."
}
