package com.example.nfctapapp.di

import android.content.Context
import com.example.nfctapapp.data.local.UserPreferences
import com.example.nfctapapp.data.remote.SupabaseClient
import com.example.nfctapapp.data.repository.AuthRepository
import com.example.nfctapapp.data.repository.AuthRepositoryImpl
import com.example.nfctapapp.data.repository.AssetBibleRepository
import com.example.nfctapapp.data.repository.BibleRepository
import com.example.nfctapapp.util.DeviceUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClient
    }

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideDeviceUtils(
        @ApplicationContext context: Context
    ): DeviceUtils {
        return DeviceUtils(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabaseClient: SupabaseClient,
        userPreferences: UserPreferences,
        deviceUtils: DeviceUtils
    ): AuthRepository {
        return AuthRepositoryImpl(supabaseClient, userPreferences, deviceUtils)
    }

    /**
     * BibleRepository 제공
     *
     * 현재: AssetBibleRepository (assets/bible/john.json 사용)
     * 향후: Room으로 확장 시 RoomBibleRepository로 전환
     */
    @Provides
    @Singleton
    fun provideBibleRepository(
        @ApplicationContext context: Context
    ): BibleRepository {
        return AssetBibleRepository(context)
    }
}
