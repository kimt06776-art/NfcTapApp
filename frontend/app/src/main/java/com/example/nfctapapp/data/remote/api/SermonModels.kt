package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
