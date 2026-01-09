package com.example.nfctapapp.data.remote.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Sermon Note API Service
 * 설교 노트 CRUD
 */
interface SermonNoteApiService {

    @POST("/api/sermon-notes")
    suspend fun createSermonNote(
        @Body request: SermonNoteCreateRequest
    ): Response<SermonNoteResponse>

    @GET("/api/sermon-notes")
    suspend fun getSermonNotes(
        @Query("userId") userId: String
    ): Response<SermonNoteListResponse>

    @GET("/api/sermon-notes/{noteId}")
    suspend fun getSermonNote(
        @Path("noteId") noteId: String
    ): Response<SermonNoteResponse>

    @PATCH("/api/sermon-notes/{noteId}")
    suspend fun updateSermonNote(
        @Path("noteId") noteId: String,
        @Body request: SermonNoteUpdateRequest
    ): Response<SermonNoteResponse>

    @DELETE("/api/sermon-notes/{noteId}")
    suspend fun deleteSermonNote(
        @Path("noteId") noteId: String
    ): Response<SermonNoteDeleteResponse>
}
