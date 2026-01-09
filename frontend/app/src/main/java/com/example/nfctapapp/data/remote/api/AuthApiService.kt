package com.example.nfctapapp.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Auth API Service
 * NFC 인증 및 사용자 등록/검증
 */
interface AuthApiService {

    @POST("/api/auth/nfc")
    suspend fun authenticateWithNfc(
        @Body request: NfcAuthRequest
    ): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun registerUser(
        @Body request: UserRegisterRequest
    ): Response<AuthResponse>

    @POST("/api/auth/validate")
    suspend fun validateUser(
        @Body request: UserValidateRequest
    ): Response<AuthResponse>
}
