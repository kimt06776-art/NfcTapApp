package com.example.nfctapapp.data.repository

import com.example.nfctapapp.data.remote.api.VoiceCommandApiService
import com.example.nfctapapp.data.remote.api.VoiceCommandAnalysis
import com.example.nfctapapp.data.remote.api.VoiceCommandRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Command Repository
 *
 * OpenAI를 통한 음성 명령 분석을 Backend API로 처리
 */
@Singleton
class VoiceCommandRepository @Inject constructor(
    private val voiceCommandApiService: VoiceCommandApiService,
    private val userId: String?
) {

    /**
     * 음성 명령 텍스트를 분석하여 실행 가능한 액션을 반환합니다.
     *
     * @param text 음성에서 변환된 텍스트
     * @return Result<VoiceCommandAnalysis> 분석 결과 또는 에러
     */
    suspend fun analyzeCommand(text: String): Result<VoiceCommandAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val response = voiceCommandApiService.analyzeVoiceCommand(
                    VoiceCommandRequest(text = text, userId = userId)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.analysis != null) {
                        Result.success(body.analysis)
                    } else {
                        Result.failure(Exception(body?.error ?: "음성 명령 분석 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
