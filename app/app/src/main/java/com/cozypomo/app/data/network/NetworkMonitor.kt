package com.cozypomo.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.data.network.di.HealthCheckClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nguồn sự thật DUY NHẤT cho trạng thái kết nối, dùng cho chấm tròn xanh/đỏ ở [CozyPomoNavHost] —
 * đỏ khi thiết bị mất mạng HOẶC có mạng nhưng không gọi được API (server sập/timeout), xanh khi
 * bình thường. Chỉ dựa vào [ConnectivityManager] là chưa đủ (thiết bị có thể "có Wi-Fi" nhưng
 * backend hoặc DNS vẫn không tới được), nên vừa lắng nghe NetworkCallback (phản ứng ngay khi mất/có
 * mạng) vừa tự ping `GET /health` định kỳ (xác nhận API thật sự phản hồi).
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @HealthCheckClient private val healthCheckClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val healthUrl: String = run {
        val base = BuildConfig.API_BASE_URL.toHttpUrl()
        base.newBuilder().encodedPath("/health").build().toString()
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    checkHealthNow()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            },
        )
        scope.launch {
            while (isActive) {
                checkHealthNow()
                kotlinx.coroutines.delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun checkHealthNow() {
        scope.launch {
            _isOnline.value = pingHealth()
        }
    }

    private fun pingHealth(): Boolean = try {
        healthCheckClient.newCall(Request.Builder().url(healthUrl).get().build()).execute().use { it.isSuccessful }
    } catch (e: IOException) {
        false
    }

    private companion object {
        const val HEALTH_CHECK_INTERVAL_MS = 15_000L
    }
}
