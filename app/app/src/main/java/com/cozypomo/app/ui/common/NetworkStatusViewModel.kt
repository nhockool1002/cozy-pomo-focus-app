package com.cozypomo.app.ui.common

import androidx.lifecycle.ViewModel
import com.cozypomo.app.data.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Chỉ expose lại [NetworkMonitor.isOnline] cho Compose — chấm tròn ở [CozyPomoNavHost]. */
@HiltViewModel
class NetworkStatusViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
}
