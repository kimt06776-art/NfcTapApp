package com.example.nfctapapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SermonDto(
    val id: String,
    @SerialName("youtube_video_id")
    val youtubeVideoId: String,
    val title: String,
    val preacher: String? = null,
    val description: String? = null,
    val scripture: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("published_at")
    val publishedAt: String,
    @SerialName("is_active")
    val isActive: Boolean = true
)
