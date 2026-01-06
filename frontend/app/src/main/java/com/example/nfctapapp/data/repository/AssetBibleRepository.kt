package com.example.nfctapapp.data.repository

import android.content.Context
import com.example.nfctapapp.data.model.BibleBook
import com.example.nfctapapp.data.model.BibleChapter
import com.example.nfctapapp.data.model.BibleVerse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assets 기반 성경 데이터 Repository
 *
 * assets/bible/john.json 파일에서 데이터를 읽어옵니다.
 *
 * 장점:
 * - 메모리 효율적 (필요할 때만 로드)
 * - JSON 파일로 데이터 관리 용이
 * - 빌드 시 앱에 포함됨
 *
 * 단점:
 * - 첫 로딩 시간 약간 증가 (무시 가능한 수준)
 * - 실시간 데이터 업데이트 불가 (서버 연동 필요 시 Room으로 전환)
 *
 * Room 전환 시:
 * - 이 클래스를 RoomBibleRepository로 교체
 * - Repository 인터페이스는 그대로 유지
 * - UI 코드 변경 불필요
 */
@Singleton
class AssetBibleRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BibleRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 캐싱: 한 번 로드한 데이터는 메모리에 유지
    private var johnBookCache: BibleBook? = null

    /**
     * assets/bible/john.json 파일 읽기
     */
    private suspend fun loadJohnGospel(): BibleBook = withContext(Dispatchers.IO) {
        // 캐시 확인
        johnBookCache?.let { return@withContext it }

        // JSON 파일 읽기
        val jsonString = context.assets.open("bible/john.json").bufferedReader().use {
            it.readText()
        }

        // 파싱
        val book = json.decodeFromString<BibleBook>(jsonString)

        // 캐싱
        johnBookCache = book
        book
    }

    override suspend fun getBook(bookName: String): BibleBook? = withContext(Dispatchers.IO) {
        if (bookName == "요한복음") {
            try {
                loadJohnGospel()
            } catch (e: Exception) {
                android.util.Log.e("AssetBibleRepository", "Failed to load book: $bookName", e)
                null
            }
        } else {
            null
        }
    }

    override suspend fun getChapter(bookName: String, chapter: Int): BibleChapter? =
        withContext(Dispatchers.IO) {
            if (bookName != "요한복음") return@withContext null

            try {
                val book = loadJohnGospel()
                book.chapters.getOrNull(chapter - 1)
            } catch (e: Exception) {
                android.util.Log.e("AssetBibleRepository", "Failed to load chapter: $chapter", e)
                null
            }
        }

    override suspend fun getVerse(bookName: String, chapter: Int, verse: Int): BibleVerse? =
        withContext(Dispatchers.IO) {
            if (bookName != "요한복음") return@withContext null

            try {
                val book = loadJohnGospel()
                book.chapters
                    .getOrNull(chapter - 1)
                    ?.verses
                    ?.getOrNull(verse - 1)
            } catch (e: Exception) {
                android.util.Log.e("AssetBibleRepository", "Failed to load verse: $chapter:$verse", e)
                null
            }
        }

    override suspend fun searchVerses(
        bookName: String?,
        keyword: String,
        ignoreCase: Boolean
    ): List<BibleVerse> = withContext(Dispatchers.IO) {
        try {
            val book = loadJohnGospel()
            val allVerses = book.chapters.flatMap { it.verses }

            allVerses.filter { verse ->
                verse.text.contains(keyword, ignoreCase = ignoreCase)
            }
        } catch (e: Exception) {
            android.util.Log.e("AssetBibleRepository", "Search failed for: $keyword", e)
            emptyList()
        }
    }

    override suspend fun getRandomVerse(bookName: String?): BibleVerse? =
        withContext(Dispatchers.IO) {
            try {
                val book = loadJohnGospel()
                val allVerses = book.chapters.flatMap { it.verses }
                allVerses.randomOrNull()
            } catch (e: Exception) {
                android.util.Log.e("AssetBibleRepository", "Failed to get random verse", e)
                null
            }
        }

    override suspend fun getAllVerses(bookName: String): List<BibleVerse> =
        withContext(Dispatchers.IO) {
            if (bookName != "요한복음") return@withContext emptyList()

            try {
                val book = loadJohnGospel()
                book.chapters.flatMap { it.verses }
            } catch (e: Exception) {
                android.util.Log.e("AssetBibleRepository", "Failed to get all verses", e)
                emptyList()
            }
        }
}
