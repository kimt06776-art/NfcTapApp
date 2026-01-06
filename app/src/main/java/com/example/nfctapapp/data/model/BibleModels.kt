package com.example.nfctapapp.data.model

import kotlinx.serialization.Serializable

/**
 * 성경 구절 데이터 모델
 *
 * Note: Room 전환 시 @Entity 어노테이션 추가 필요
 * @Entity(tableName = "bible_verses")
 */
@Serializable
data class BibleVerse(
    val book: String,           // 책 이름 (예: "요한복음")
    val chapter: Int,           // 장
    val verse: Int,             // 절
    val text: String            // 본문
) {
    val reference: String
        get() = "$book $chapter:$verse"
}

/**
 * 성경 장 데이터 모델
 *
 * Note: Room 전환 시 Embedded 관계로 변경 고려
 */
@Serializable
data class BibleChapter(
    val book: String,
    val chapter: Int,
    val verses: List<BibleVerse>
)

/**
 * 성경 책 데이터 모델
 *
 * Note: Room 전환 시 별도 테이블로 분리 고려
 * @Entity(tableName = "bible_books")
 */
@Serializable
data class BibleBook(
    val name: String,
    val abbreviation: String,   // 약어 (예: "요")
    val totalChapters: Int,
    val chapters: List<BibleChapter>
) {
    val totalVerses: Int
        get() = chapters.sumOf { it.verses.size }
}
