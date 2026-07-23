package com.cozypomo.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** S-00 — quyết định điểm vào kế tiếp: Onboarding (lần đầu) / Login (chưa có JWT) / Main. */
enum class SplashDestination { Onboarding, Login, Main }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
            _destination.value = when {
                !seenOnboarding -> SplashDestination.Onboarding
                !loggedIn -> SplashDestination.Login
                else -> SplashDestination.Main
            }
        }
    }
}
