package com.example.nfctapapp.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Chat API Service
 * AI 채팅 세션 및 메시지 관리
 */
interface ChatApiService {

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
}
