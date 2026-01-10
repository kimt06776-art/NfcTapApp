package com.example.nfctapapp.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Chat API Service
 * AI 채팅 세션 및 메시지 관리
 *
 * Agent 기반 아키텍처:
 * - agentChat: 모든 요청을 처리하는 메인 엔드포인트
 * - Agent가 자동으로 도구 사용 여부 결정
 */
interface ChatApiService {

    /**
     * Main Agent Chat endpoint
     * Agent가 자동으로 판단하여 응답 생성
     * - 성경 검색, 화면 네비게이션, 상담 등 모두 처리
     */
    @POST("/api/chat/agent")
    suspend fun agentChat(
        @Body request: ChatStreamRequest
    ): Response<AgentChatResponse>

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
