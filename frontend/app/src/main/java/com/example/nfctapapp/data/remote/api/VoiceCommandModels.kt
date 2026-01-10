package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
