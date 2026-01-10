package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatStreamRequest(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "userMessage") val userMessage: String
)

@JsonClass(generateAdapter = true)
data class SessionCreateRequest(
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String?
)

@JsonClass(generateAdapter = true)
data class SessionUpdateRequest(
    @Json(name = "title") val title: String
)

@JsonClass(generateAdapter = true)
data class ChatSessionDto(
    @Json(name = "id") val id: String?,
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: String?,
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "content") val content: String,
    @Json(name = "isFromUser") val isFromUser: Boolean,
    @Json(name = "createdAt") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class ChatMessageInsert(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "content") val content: String,
    @Json(name = "isFromUser") val isFromUser: Boolean
)

@JsonClass(generateAdapter = true)
data class SessionListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "sessions") val sessions: List<ChatSessionDto>?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class MessageListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "messages") val messages: List<ChatMessageDto>?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class SessionCreateResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "session") val session: ChatSessionDto?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class MessageCreateResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: ChatMessageDto?,
    @Json(name = "error") val error: String?
)

// ==================== Intent Classification ====================

enum class IntentType {
    @Json(name = "bible_study") BIBLE_STUDY,
    @Json(name = "bible_reading") BIBLE_READING,
    @Json(name = "sermon_note") SERMON_NOTE,
    @Json(name = "counseling") COUNSELING,
    @Json(name = "prayer") PRAYER,
    @Json(name = "general") GENERAL
}

@JsonClass(generateAdapter = true)
data class IntentClassificationDto(
    @Json(name = "intent") val intent: String,
    @Json(name = "confidence") val confidence: Double,
    @Json(name = "reasoning") val reasoning: String
)

@JsonClass(generateAdapter = true)
data class ClassifyIntentResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "classification") val classification: IntentClassificationDto?,
    @Json(name = "error") val error: String? = null
)

// ==================== Bible Study Response ====================

@JsonClass(generateAdapter = true)
data class BibleStudyData(
    @Json(name = "passage_explanation") val passageExplanation: String,
    @Json(name = "historical_context") val historicalContext: String,
    @Json(name = "key_words") val keyWords: List<String>,
    @Json(name = "life_application") val lifeApplication: String,
    @Json(name = "reflection_questions") val reflectionQuestions: List<String>,
    @Json(name = "related_verses") val relatedVerses: List<String>
)

@JsonClass(generateAdapter = true)
data class BibleStudyResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "response_type") val responseType: String,
    @Json(name = "intent") val intent: String,
    @Json(name = "data") val data: BibleStudyData?
)

// ==================== Bible Reading Response ====================

@JsonClass(generateAdapter = true)
data class BibleReadingData(
    @Json(name = "book") val book: String,
    @Json(name = "chapter") val chapter: Int?,
    @Json(name = "verse") val verse: Int?,
    @Json(name = "verse_end") val verseEnd: Int?,
    @Json(name = "is_supported") val isSupported: Boolean = true,
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class BibleReadingResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "response_type") val responseType: String,
    @Json(name = "intent") val intent: String,
    @Json(name = "data") val data: BibleReadingData?,
    @Json(name = "error") val error: String? = null
)

// ==================== Agent Chat Response ====================

@JsonClass(generateAdapter = true)
data class NavigationInfo(
    @Json(name = "screen") val screen: String,
    @Json(name = "params") val params: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class AgentChatResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "response") val response: String?,
    @Json(name = "navigation") val navigation: NavigationInfo?,
    @Json(name = "tools_used") val toolsUsed: List<String>?,
    @Json(name = "error") val error: String? = null
)
