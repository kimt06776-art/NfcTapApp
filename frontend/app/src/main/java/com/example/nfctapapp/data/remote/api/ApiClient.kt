package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Retrofit API Client Configuration
 *
 * JavaScript의 axios.create()와 동일한 역할
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiClient {

    /**
     * Backend API Base URL
     *
     * Development: http://10.0.2.2:8000 (Android Emulator → localhost)
     * Production: Render 배포 서버
     */
    private const val BASE_URL = "https://nfctapapp-backend.onrender.com"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 개발 시에만 사용
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // ==================== 도메인별 ApiService ====================

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSermonApiService(retrofit: Retrofit): SermonApiService {
        return retrofit.create(SermonApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVoiceCommandApiService(retrofit: Retrofit): VoiceCommandApiService {
        return retrofit.create(VoiceCommandApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSermonNoteApiService(retrofit: Retrofit): SermonNoteApiService {
        return retrofit.create(SermonNoteApiService::class.java)
    }
}
