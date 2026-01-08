package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==================== Auth API Models ====================

@JsonClass(generateAdapter = true)
data class NfcAuthRequest(
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class UserRegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceName") val deviceName: String?
)

@JsonClass(generateAdapter = true)
data class UserValidateRequest(
    @Json(name = "userId") val userId: String,
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "user") val user: UserDto?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "created_at") val createdAt: String?
)

// ==================== Chat API Models ====================

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

// ==================== Sermon API Models ====================

@JsonClass(generateAdapter = true)
data class SermonDto(
    @Json(name = "id") val id: String,
    @Json(name = "youtube_video_id") val youtubeVideoId: String,
    @Json(name = "title") val title: String,
    @Json(name = "preacher") val preacher: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "scripture") val scripture: String?,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?,
    @Json(name = "published_at") val publishedAt: String,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SermonListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "sermons") val sermons: List<SermonDto>?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class SermonDetailResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "sermon") val sermon: SermonDto?,
    @Json(name = "error") val error: String?
)

// ==================== Voice Command API Models ====================

enum class VoiceCommandAction {
    @Json(name = "navigate_home") NAVIGATE_HOME,
    @Json(name = "navigate_sermon") NAVIGATE_SERMON,
    @Json(name = "navigate_chat") NAVIGATE_CHAT,
    @Json(name = "navigate_pathway") NAVIGATE_PATHWAY,
    @Json(name = "show_daily_verse") SHOW_DAILY_VERSE,
    @Json(name = "start_chat") START_CHAT,
    @Json(name = "play_latest_sermon") PLAY_LATEST_SERMON,
    @Json(name = "unknown") UNKNOWN
}

@JsonClass(generateAdapter = true)
data class VoiceCommandRequest(
    @Json(name = "text") val text: String,
    @Json(name = "userId") val userId: String?
)

@JsonClass(generateAdapter = true)
data class VoiceCommandAnalysis(
    @Json(name = "action") val action: VoiceCommandAction,
    @Json(name = "confidence") val confidence: Double,
    @Json(name = "parameters") val parameters: Map<String, Any>?,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class VoiceCommandResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "analysis") val analysis: VoiceCommandAnalysis?,
    @Json(name = "error") val error: String?
)
