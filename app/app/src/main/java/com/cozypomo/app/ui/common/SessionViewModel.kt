package com.cozypomo.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Quan sát `isLoggedIn` (đọc thẳng từ DataStore qua AuthRepository) ở tầng NavHost — khi
 * [TokenAuthenticator][com.cozypomo.app.data.network.TokenAuthenticator] phải xoá phiên vì
 * refresh token cũng hết hạn (không phải người dùng tự bấm Đăng xuất), Flow này tự chuyển
 * `false` và CozyPomoNavHost điều hướng về Login — không cần đợi người dùng chạm vào API nào
 * khác mới nhận ra đã bị đăng xuất.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
