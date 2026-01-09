package com.example.nfctapapp.data

import com.example.nfctapapp.data.remote.api.SermonApiService
import com.example.nfctapapp.data.remote.api.SermonDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

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

/**
 * 샘플 설교 데이터 (하드코딩)
 * HomeScreen에서 빠른 프로토타이핑용
 */
object SampleSermonData {
    fun getSampleSermons(): List<Sermon> {
        return listOf(
            Sermon(
                id = "1",
                title = "하나님의 사랑과 은혜",
                preacher = "김목사",
                date = "2024.01.14",
                description = "요한복음 3장 16절을 통해 하나님의 무한하신 사랑을 묵상합니다.",
                youtubeVideoId = "dQw4w9WgXcQ",
                scripture = "요한복음 3:16"
            ),
            Sermon(
                id = "2",
                title = "믿음으로 사는 삶",
                preacher = "이목사",
                date = "2024.01.07",
                description = "히브리서 11장을 통해 믿음의 본질을 배웁니다.",
                youtubeVideoId = "dQw4w9WgXcQ",
                scripture = "히브리서 11:1-6"
            )
        )
    }
}

/**
 * Sermon Repository (Backend API 사용)
 *
 * 이전: SupabaseClient 직접 사용
 * 현재: Backend REST API 사용 (보안 강화)
 */
@Singleton
class SermonRepository @Inject constructor(
    private val sermonApiService: SermonApiService
) {

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
        return withContext(Dispatchers.IO) {
            try {
                val response = sermonApiService.getAllSermons()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val sermons = body.sermons?.map { it.toSermon() } ?: emptyList()
                        Result.success(sermons)
                    } else {
                        Result.failure(Exception(body?.error ?: "설교 조회 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getLatestSermon(): Result<Sermon?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sermonApiService.getLatestSermon()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val sermon = body.sermon?.toSermon()
                        Result.success(sermon)
                    } else {
                        Result.failure(Exception(body?.error ?: "최신 설교 조회 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getSermonById(id: String): Result<Sermon?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sermonApiService.getSermonById(id)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val sermon = body.sermon?.toSermon()
                        Result.success(sermon)
                    } else {
                        Result.failure(Exception(body?.error ?: "설교 조회 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
