package com.example.nfctapapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val title: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ChatMessageDto(
    val id: String? = null,
    @SerialName("session_id")
    val sessionId: String,
    val content: String,
    @SerialName("is_from_user")
    val isFromUser: Boolean,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ChatSessionInsert(
    @SerialName("user_id")
    val userId: String,
    val title: String? = null
)

@Serializable
data class ChatMessageInsert(
    @SerialName("session_id")
    val sessionId: String,
    val content: String,
    @SerialName("is_from_user")
    val isFromUser: Boolean
)
