package com.example.nfctapapp.data

import com.example.nfctapapp.data.remote.SupabaseClient
import com.example.nfctapapp.data.remote.dto.SermonDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Sermon(
    val id: String,
    val title: String,
    val preacher: String,
    val date: String,
    val description: String,
    val youtubeVideoId: String,
    val scripture: String,
    val thumbnailUrl: String? = null
)

object SermonRepository {

    private fun SermonDto.toSermon(): Sermon {
        // ISO 날짜를 "2024.12.29" 형식으로 변환
        val formattedDate = try {
            val zonedDateTime = ZonedDateTime.parse(publishedAt)
            zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        } catch (e: Exception) {
            publishedAt.take(10).replace("-", ".")
        }

        return Sermon(
            id = id,
            title = title,
            preacher = preacher ?: "목사",
            date = formattedDate,
            description = description ?: "",
            youtubeVideoId = youtubeVideoId,
            scripture = scripture ?: "",
            thumbnailUrl = thumbnailUrl
        )
    }

    suspend fun getAllSermons(): Result<List<Sermon>> {
        return try {
            val response = SupabaseClient.client.postgrest
                .from("sermons")
                .select {
                    filter { eq("is_active", true) }
                    order("published_at", Order.DESCENDING)
                }
                .decodeList<SermonDto>()

            Result.success(response.map { it.toSermon() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestSermon(): Result<Sermon?> {
        return try {
            val response = SupabaseClient.client.postgrest
                .from("sermons")
                .select {
                    filter { eq("is_active", true) }
                    order("published_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SermonDto>()

            Result.success(response.firstOrNull()?.toSermon())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSermonById(id: String): Result<Sermon?> {
        return try {
            val response = SupabaseClient.client.postgrest
                .from("sermons")
                .select {
                    filter { eq("id", id) }
                }
                .decodeList<SermonDto>()

            Result.success(response.firstOrNull()?.toSermon())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
