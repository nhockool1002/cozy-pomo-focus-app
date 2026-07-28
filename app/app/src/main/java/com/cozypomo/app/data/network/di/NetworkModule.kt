package com.cozypomo.app.data.network.di

import com.cozypomo.app.BuildConfig
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.AuthInterceptor
import com.cozypomo.app.data.network.RefreshApiService
import com.cozypomo.app.data.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** OkHttpClient RIÊNG cho [RefreshApiService] — không gắn [TokenAuthenticator] (tránh đệ quy vô hạn). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshHttpClient

/** OkHttpClient RIÊNG cho [com.cozypomo.app.data.network.NetworkMonitor] — không gắn AuthInterceptor/
 * TokenAuthenticator (chấm trạng thái không nên tự refresh token/logout) và timeout ngắn để chấm đỏ
 * hiện nhanh khi API không phản hồi thay vì treo theo timeout mặc định (thường dài hơn nhiều). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HealthCheckClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor())
            .build()

    @RefreshHttpClient
    @Provides
    @Singleton
    fun provideRefreshOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()

    @HealthCheckClient
    @Provides
    @Singleton
    fun provideHealthCheckOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideRefreshApiService(@RefreshHttpClient okHttpClient: OkHttpClient, json: Json): RefreshApiService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RefreshApiService::class.java)
}
