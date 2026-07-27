package com.cozypomo.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.data.auth.AuthRepository
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.ui.forceupdate.ForceUpdateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** S-00 — quyết định điểm vào kế tiếp: Onboarding (lần đầu) / Login (chưa có JWT) / Main /
 * ForceUpdate (T-122, versionCode hiện tại thấp hơn `minSupportedVersionCode`). */
enum class SplashDestination { Onboarding, Login, Main, ForceUpdate }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
    private val forceUpdateHolder: ForceUpdateHolder,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // Splash hiện đủ lâu để thấy hoạt ảnh chào mừng — không chớp qua ngay cả khi
            // restoreSession() xong rất nhanh (thường có JWT sẵn trong DataStore).
            val minDurationMs = 2200L
            val start = System.currentTimeMillis()
            authRepository.restoreSession()
            val seenOnboarding = authRepository.hasSeenOnboarding.first()
            val loggedIn = authRepository.isLoggedIn.first()

            // Fail-open: nếu backend không phản hồi được (mất mạng/server down), KHÔNG chặn user
            // vào app — chỉ chặn khi biết chắc versionCode hiện tại dưới ngưỡng tối thiểu.
            val forceUpdate = runCatching {
                val config = apiService.getAppVersion()
                if (BuildConfig.VERSION_CODE < config.minSupportedVersionCode) {
                    forceUpdateHolder.config = config
                    true
                } else {
                    false
                }
            }.getOrDefault(false)

            val elapsed = System.currentTimeMillis() - start
            if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
            _destination.value = when {
                forceUpdate -> SplashDestination.ForceUpdate
                !seenOnboarding -> SplashDestination.Onboarding
                !loggedIn -> SplashDestination.Login
                else -> SplashDestination.Main
            }
        }
    }
}
