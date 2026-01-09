package com.example.nfctapapp.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Voice Command API Service
 * 음성 명령 분석
 */
interface VoiceCommandApiService {

    @POST("/api/voice-command")
    suspend fun analyzeVoiceCommand(
        @Body request: VoiceCommandRequest
    ): Response<VoiceCommandResponse>
}
