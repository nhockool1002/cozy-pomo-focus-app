package com.cozypomo.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Đánh dấu đã xem Onboarding rồi mới gọi [onDone] — chỉ hiện lại nếu cài app mới. */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.completeOnboarding()
            onDone()
        }
    }
}
