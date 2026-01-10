package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SermonNoteDto(
    @Json(name = "id") val id: String?,
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "scripture") val scripture: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class SermonNoteCreateRequest(
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "scripture") val scripture: String?
)

@JsonClass(generateAdapter = true)
data class SermonNoteUpdateRequest(
    @Json(name = "title") val title: String?,
    @Json(name = "content") val content: String?,
    @Json(name = "scripture") val scripture: String?
)

@JsonClass(generateAdapter = true)
data class SermonNoteListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "notes") val notes: List<SermonNoteDto>?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class SermonNoteResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "note") val note: SermonNoteDto?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class SermonNoteDeleteResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "error") val error: String?
)
