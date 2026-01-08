package com.example.nfctapapp.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Backend API Service Interface
 *
 * JavaScript의 api.js와 동일한 역할
 */
interface ApiService {

    // ==================== Auth API ====================

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

    // ==================== Chat API ====================

    /**
     * Chat streaming endpoint
     * Returns Server-Sent Events (SSE) stream
     */
    @Streaming
    @POST("/api/chat/stream")
    suspend fun chatStream(
        @Body request: ChatStreamRequest
    ): Response<ResponseBody>

    @POST("/api/chat/sessions")
    suspend fun createSession(
        @Body request: SessionCreateRequest
    ): Response<SessionCreateResponse>

    @GET("/api/chat/sessions")
    suspend fun getSessions(
        @Query("userId") userId: String
    ): Response<SessionListResponse>

    @PATCH("/api/chat/sessions/{sessionId}")
    suspend fun updateSessionTitle(
        @Path("sessionId") sessionId: String,
        @Body request: SessionUpdateRequest
    ): Response<Map<String, Any>>

    @DELETE("/api/chat/sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: String
    ): Response<Map<String, Any>>

    @GET("/api/chat/sessions/{sessionId}/messages")
    suspend fun getMessages(
        @Path("sessionId") sessionId: String
    ): Response<MessageListResponse>

    @POST("/api/chat/messages")
    suspend fun saveMessage(
        @Body request: ChatMessageInsert
    ): Response<MessageCreateResponse>

    // ==================== Sermon API ====================

    @GET("/api/sermons")
    suspend fun getAllSermons(): Response<SermonListResponse>

    @GET("/api/sermons/latest")
    suspend fun getLatestSermon(): Response<SermonDetailResponse>

    @GET("/api/sermons/{sermonId}")
    suspend fun getSermonById(
        @Path("sermonId") sermonId: String
    ): Response<SermonDetailResponse>

    // ==================== Voice Command API ====================

    @POST("/api/voice-command")
    suspend fun analyzeVoiceCommand(
        @Body request: VoiceCommandRequest
    ): Response<VoiceCommandResponse>
}
