package com.cozypomo.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requiresAuth = original.header("Requires-Auth") != null
        if (!requiresAuth) {
            return chain.proceed(original)
        }

        val requestBuilder = original.newBuilder().removeHeader("Requires-Auth")
        tokenProvider.accessToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
