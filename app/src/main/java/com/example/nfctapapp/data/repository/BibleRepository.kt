package com.example.nfctapapp.data.repository

import com.example.nfctapapp.data.model.BibleBook
import com.example.nfctapapp.data.model.BibleChapter
import com.example.nfctapapp.data.model.BibleVerse

/**
 * 성경 데이터 접근 인터페이스
 *
 * Repository 패턴을 사용하여 데이터 소스를 추상화합니다.
 * 구현체:
 * 1. InMemoryBibleRepository - 기존 BibleData.kt 사용 (현재)
 * 2. AssetBibleRepository - JSON 파일 사용 (향후 전환)
 * 3. RoomBibleRepository - Room Database 사용 (최종 확장 시)
 *
 * 장점:
 * - UI 코드는 데이터 소스와 무관하게 동일한 인터페이스 사용
 * - 데이터 소스 변경 시 구현체만 교체하면 됨
 * - 테스트 시 Mock Repository 사용 가능
 */
interface BibleRepository {

    /**
     * 특정 책의 전체 데이터 가져오기
     * @param bookName 책 이름 (예: "요한복음")
     * @return 해당 책의 전체 데이터, 없으면 null
     */
    suspend fun getBook(bookName: String): BibleBook?

    /**
     * 특정 장 가져오기
     * @param bookName 책 이름
     * @param chapter 장 번호 (1부터 시작)
     * @return 해당 장 데이터, 없으면 null
     */
    suspend fun getChapter(bookName: String, chapter: Int): BibleChapter?

    /**
     * 특정 구절 가져오기
     * @param bookName 책 이름
     * @param chapter 장 번호
     * @param verse 절 번호 (1부터 시작)
     * @return 해당 구절 데이터, 없으면 null
     */
    suspend fun getVerse(bookName: String, chapter: Int, verse: Int): BibleVerse?

    /**
     * 키워드로 구절 검색
     * @param bookName 책 이름 (null이면 전체 검색)
     * @param keyword 검색어
     * @param ignoreCase 대소문자 무시 여부
     * @return 검색 결과 구절 리스트
     */
    suspend fun searchVerses(
        bookName: String? = null,
        keyword: String,
        ignoreCase: Boolean = true
    ): List<BibleVerse>

    /**
     * 랜덤 구절 가져오기
     * @param bookName 책 이름 (null이면 전체에서 랜덤)
     * @return 랜덤 구절
     */
    suspend fun getRandomVerse(bookName: String? = null): BibleVerse?

    /**
     * 특정 책의 전체 구절 리스트
     * @param bookName 책 이름
     * @return 전체 구절 리스트
     */
    suspend fun getAllVerses(bookName: String): List<BibleVerse>
}
