package com.example.nfctapapp.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Sermon API Service
 * 설교 영상 목록 및 상세 조회
 */
interface SermonApiService {

    @GET("/api/sermons")
    suspend fun getAllSermons(): Response<SermonListResponse>

    @GET("/api/sermons/latest")
    suspend fun getLatestSermon(): Response<SermonDetailResponse>

    @GET("/api/sermons/{sermonId}")
    suspend fun getSermonById(
        @Path("sermonId") sermonId: String
    ): Response<SermonDetailResponse>
}
