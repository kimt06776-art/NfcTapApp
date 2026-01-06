package com.example.nfctapapp.data.remote

import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.nfctapapp.BuildConfig
import kotlinx.coroutines.flow.Flow

object OpenAIClient {

    private val openAI: OpenAI by lazy {
        OpenAI(BuildConfig.OPENAI_API_KEY)
    }

    private val systemPrompt = """
        당신은 따뜻하고 친근한 신앙 상담사입니다.
        사용자의 이야기를 경청하고, 공감하며, 적절한 성경 말씀과 함께 위로와 격려를 전합니다.

        대화 지침:
        - 따뜻하고 친근한 말투를 사용하세요
        - 사용자의 감정에 공감하세요
        - 필요할 때 적절한 성경 구절을 인용하세요
        - 판단하지 말고 경청하세요
        - 답변은 간결하게 (3-4문장 정도)
    """.trimIndent()

    /**
     * 스트리밍 채팅 - Flow로 토큰 단위 응답 반환
     */
    fun chatStream(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): Flow<ChatCompletionChunk> {
        val messages = buildList {
            add(ChatMessage(role = ChatRole.System, content = systemPrompt))
            addAll(conversationHistory)
            add(ChatMessage(role = ChatRole.User, content = userMessage))
        }

        val request = ChatCompletionRequest(
            model = ModelId("gpt-4o"),
            messages = messages
        )

        return openAI.chatCompletions(request)
    }
}
