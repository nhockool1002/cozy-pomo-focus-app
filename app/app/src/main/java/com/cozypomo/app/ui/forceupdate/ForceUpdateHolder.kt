package com.cozypomo.app.ui.forceupdate

import com.cozypomo.app.data.network.AppVersionDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kênh chuyển tiếp trong-tiến-trình từ [com.cozypomo.app.ui.splash.SplashViewModel] sang
 * [ForceUpdateScreen] — 2 route khác nhau trong `RootNavHost` nên `hiltViewModel()` của mỗi màn
 * là instance riêng, không share state trực tiếp được; dùng singleton nhẹ này thay vì mã hoá
 * updateTitle/updateMessage (tiếng Việt có dấu) vào query param của route, đơn giản hơn nhiều.
 * Chỉ cần đúng cho 1 lần điều hướng Splash → ForceUpdate (không cần chịu được process death vì
 * app khởi động lại từ Splash sẽ tự gọi lại API).
 */
@Singleton
class ForceUpdateHolder @Inject constructor() {
    @Volatile
    var config: AppVersionDto = AppVersionDto()
}
