package com.example.nfctapapp.data.repository

import com.example.nfctapapp.data.remote.api.BibleReadingData
import com.example.nfctapapp.data.remote.api.BibleStudyData
import com.example.nfctapapp.data.remote.api.ChatApiService
import com.example.nfctapapp.data.remote.api.ChatMessageDto
import com.example.nfctapapp.data.remote.api.ChatMessageInsert
import com.example.nfctapapp.data.remote.api.ChatSessionDto
import com.example.nfctapapp.data.remote.api.ChatStreamRequest
import com.example.nfctapapp.data.remote.api.IntentClassificationDto
import com.example.nfctapapp.data.remote.api.SessionCreateRequest
import com.example.nfctapapp.data.remote.api.SessionUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat Repository (Backend API 사용)
 *
 * 이전: OpenAIClient + SupabaseClient 직접 사용
 * 현재: Backend REST API 사용 (보안 강화)
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatApiService: ChatApiService,
    private val userId: String
) {

    // ==================== 세션 관련 ====================

    suspend fun createSession(title: String? = null): Result<ChatSessionDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.createSession(
                    SessionCreateRequest(userId = userId, title = title)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.session != null) {
                        Result.success(body.session)
                    } else {
                        Result.failure(Exception(body?.error ?: "세션 생성 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getSessions(): Result<List<ChatSessionDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.getSessions(userId)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Result.success(body.sessions ?: emptyList())
                    } else {
                        Result.failure(Exception(body?.error ?: "세션 조회 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateSessionTitle(sessionId: String, title: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.updateSessionTitle(
                    sessionId,
                    SessionUpdateRequest(title = title)
                )

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.deleteSession(sessionId)

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== 메시지 관련 ====================

    suspend fun getMessages(sessionId: String): Result<List<ChatMessageDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.getMessages(sessionId)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Result.success(body.messages ?: emptyList())
                    } else {
                        Result.failure(Exception(body?.error ?: "메시지 조회 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun saveMessage(
        sessionId: String,
        content: String,
        isFromUser: Boolean
    ): Result<ChatMessageDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.saveMessage(
                    ChatMessageInsert(
                        sessionId = sessionId,
                        content = content,
                        isFromUser = isFromUser
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.message != null) {
                        Result.success(body.message)
                    } else {
                        Result.failure(Exception(body?.error ?: "메시지 저장 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== Intent Classification ====================

    /**
     * 사용자 메시지 의도 분류
     * bible_study, counseling, prayer, general 중 하나 반환
     */
    suspend fun classifyIntent(userMessage: String): Result<IntentClassificationDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.classifyIntent(
                    ChatStreamRequest(sessionId = "", userMessage = userMessage)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.classification != null) {
                        Result.success(body.classification)
                    } else {
                        Result.failure(Exception(body?.error ?: "의도 분류 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 성경 공부 응답 요청
     * Structured output으로 정형화된 응답 반환
     */
    suspend fun getBibleStudy(sessionId: String, userMessage: String): Result<BibleStudyData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.bibleStudy(
                    ChatStreamRequest(sessionId = sessionId, userMessage = userMessage)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception("성경 공부 응답 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 성경 읽기 요청 파싱
     * 책/장/절 정보를 추출하여 네비게이션에 사용
     */
    suspend fun getBibleReading(userMessage: String): Result<BibleReadingData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatApiService.bibleReading(
                    ChatStreamRequest(sessionId = "", userMessage = userMessage)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        // 지원되는 성경책인지 확인
                        if (body.data.isSupported && body.data.chapter != null) {
                            Result.success(body.data)
                        } else {
                            // 지원되지 않는 책이거나 chapter가 없는 경우
                            val errorMsg = body.data.errorMessage
                                ?: body.error
                                ?: "${body.data.book}은(는) 현재 지원하지 않는 성경책입니다"
                            Result.failure(Exception(errorMsg))
                        }
                    } else {
                        // success가 false인 경우
                        val errorMsg = body?.error ?: "성경 구절 파싱 실패"
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== AI 대화 (스트리밍) ====================

    /**
     * 서버로부터 SSE (Server-Sent Events) 스트리밍 응답을 받아 처리
     *
     * 백엔드 POST /api/chat/stream 엔드포인트 호출
     */
    fun sendMessageStream(sessionId: String, userMessage: String): Flow<StreamResponse> = flow {
        try {
            val response = chatApiService.chatStream(
                ChatStreamRequest(
                    sessionId = sessionId,
                    userMessage = userMessage
                )
            )

            if (!response.isSuccessful) {
                emit(StreamResponse.Error("HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }

            val body = response.body()
            if (body == null) {
                emit(StreamResponse.Error("응답 본문이 비어있습니다"))
                return@flow
            }

            // SSE 스트림 파싱
            val fullResponse = StringBuilder()
            parseSseStream(body) { chunk ->
                fullResponse.append(chunk)
                emit(StreamResponse.Streaming(fullResponse.toString()))
            }

            emit(StreamResponse.Complete(fullResponse.toString()))

        } catch (e: Exception) {
            emit(StreamResponse.Error(e.message ?: "메시지 전송에 실패했습니다"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * SSE (Server-Sent Events) 스트림 파싱
     *
     * 형식:
     * data: 안녕
     * data: 하세요
     * data: [DONE]
     */
    private suspend fun parseSseStream(
        body: ResponseBody,
        onChunk: suspend (String) -> Unit
    ) {
        body.source().use { source ->
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break

                // SSE 형식: "data: <content>"
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()

                    // [DONE] 시그널 체크
                    if (data == "[DONE]") {
                        break
                    }

                    // 텍스트 그대로 처리 (JSON 파싱 불필요)
                    if (data.isNotEmpty()) {
                        onChunk(data)
                    }
                }
            }
        }
    }
}

/**
 * 스트리밍 응답 상태
 */
sealed class StreamResponse {
    data class Streaming(val content: String) : StreamResponse()
    data class Complete(val content: String) : StreamResponse()
    data class Error(val message: String) : StreamResponse()
}
