package com.example.nfctapapp.data.repository

import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.example.nfctapapp.data.remote.OpenAIClient
import com.example.nfctapapp.data.remote.SupabaseClient
import com.example.nfctapapp.data.remote.dto.ChatMessageDto
import com.example.nfctapapp.data.remote.dto.ChatMessageInsert
import com.example.nfctapapp.data.remote.dto.ChatSessionDto
import com.example.nfctapapp.data.remote.dto.ChatSessionInsert
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatRepository(private val userId: String) {

    private val supabase = SupabaseClient.client
    private val conversationHistory = mutableListOf<OpenAIChatMessage>()

    // 세션 관련
    suspend fun createSession(title: String? = null): Result<ChatSessionDto> {
        return try {
            val session = supabase.postgrest["chat_sessions"]
                .insert(ChatSessionInsert(userId = userId, title = title)) {
                    select()
                }
                .decodeSingle<ChatSessionDto>()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessions(): Result<List<ChatSessionDto>> {
        return try {
            val sessions = supabase.postgrest["chat_sessions"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("updated_at", Order.DESCENDING)
                }
                .decodeList<ChatSessionDto>()
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSessionTitle(sessionId: String, title: String): Result<Unit> {
        return try {
            supabase.postgrest["chat_sessions"]
                .update({ set("title", title) }) {
                    filter {
                        eq("id", sessionId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            supabase.postgrest["chat_sessions"]
                .delete {
                    filter {
                        eq("id", sessionId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 메시지 관련
    suspend fun getMessages(sessionId: String): Result<List<ChatMessageDto>> {
        return try {
            val messages = supabase.postgrest["chat_messages"]
                .select {
                    filter {
                        eq("session_id", sessionId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ChatMessageDto>()

            // 대화 히스토리 복원
            conversationHistory.clear()
            messages.forEach { msg ->
                conversationHistory.add(
                    OpenAIChatMessage(
                        role = if (msg.isFromUser) ChatRole.User else ChatRole.Assistant,
                        content = msg.content
                    )
                )
            }

            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMessage(sessionId: String, content: String, isFromUser: Boolean): Result<ChatMessageDto> {
        return try {
            val message = supabase.postgrest["chat_messages"]
                .insert(ChatMessageInsert(
                    sessionId = sessionId,
                    content = content,
                    isFromUser = isFromUser
                )) {
                    select()
                }
                .decodeSingle<ChatMessageDto>()

            // 세션 업데이트 시간 갱신
            supabase.postgrest["chat_sessions"]
                .update({ set("updated_at", "now()") }) {
                    filter {
                        eq("id", sessionId)
                    }
                }

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // AI 대화 (스트리밍)
    fun sendMessageStream(sessionId: String, userMessage: String): Flow<StreamResponse> = flow {
        // 사용자 메시지 저장
        saveMessage(sessionId, userMessage, isFromUser = true)

        val fullResponse = StringBuilder()

        try {
            // 스트리밍 응답 받기
            OpenAIClient.chatStream(userMessage, conversationHistory).collect { chunk ->
                val content = chunk.choices.firstOrNull()?.delta?.content
                if (content != null) {
                    fullResponse.append(content)
                    emit(StreamResponse.Streaming(fullResponse.toString()))
                }
            }

            val response = fullResponse.toString()

            // 대화 히스토리에 추가
            conversationHistory.add(OpenAIChatMessage(role = ChatRole.User, content = userMessage))
            conversationHistory.add(OpenAIChatMessage(role = ChatRole.Assistant, content = response))

            // AI 응답 저장
            saveMessage(sessionId, response, isFromUser = false)

            emit(StreamResponse.Complete(response))
        } catch (e: Exception) {
            emit(StreamResponse.Error(e.message ?: "메시지 전송에 실패했습니다"))
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}

sealed class StreamResponse {
    data class Streaming(val content: String) : StreamResponse()
    data class Complete(val content: String) : StreamResponse()
    data class Error(val message: String) : StreamResponse()
}
