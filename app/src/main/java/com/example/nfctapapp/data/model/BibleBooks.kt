package com.example.nfctapapp.data.model

/**
 * 성경 66권 정보
 */
data class BookInfo(
    val name: String,           // 한글 이름 (예: "창세기")
    val abbreviation: String,   // 약어 (예: "창")
    val totalChapters: Int,     // 총 장 수
    val testament: Testament    // 구약/신약
)

enum class Testament {
    OLD,  // 구약
    NEW   // 신약
}

/**
 * 성경 66권 전체 목록
 */
object BibleBooks {

    // 구약 39권
    private val oldTestament = listOf(
        BookInfo("창세기", "창", 50, Testament.OLD),
        BookInfo("출애굽기", "출", 40, Testament.OLD),
        BookInfo("레위기", "레", 27, Testament.OLD),
        BookInfo("민수기", "민", 36, Testament.OLD),
        BookInfo("신명기", "신", 34, Testament.OLD),
        BookInfo("여호수아", "수", 24, Testament.OLD),
        BookInfo("사사기", "삿", 21, Testament.OLD),
        BookInfo("룻기", "룻", 4, Testament.OLD),
        BookInfo("사무엘상", "삼상", 31, Testament.OLD),
        BookInfo("사무엘하", "삼하", 24, Testament.OLD),
        BookInfo("열왕기상", "왕상", 22, Testament.OLD),
        BookInfo("열왕기하", "왕하", 25, Testament.OLD),
        BookInfo("역대상", "대상", 29, Testament.OLD),
        BookInfo("역대하", "대하", 36, Testament.OLD),
        BookInfo("에스라", "스", 10, Testament.OLD),
        BookInfo("느헤미야", "느", 13, Testament.OLD),
        BookInfo("에스더", "에", 10, Testament.OLD),
        BookInfo("욥기", "욥", 42, Testament.OLD),
        BookInfo("시편", "시", 150, Testament.OLD),
        BookInfo("잠언", "잠", 31, Testament.OLD),
        BookInfo("전도서", "전", 12, Testament.OLD),
        BookInfo("아가", "아", 8, Testament.OLD),
        BookInfo("이사야", "사", 66, Testament.OLD),
        BookInfo("예레미야", "렘", 52, Testament.OLD),
        BookInfo("예레미야애가", "애", 5, Testament.OLD),
        BookInfo("에스겔", "겔", 48, Testament.OLD),
        BookInfo("다니엘", "단", 12, Testament.OLD),
        BookInfo("호세아", "호", 14, Testament.OLD),
        BookInfo("요엘", "욜", 3, Testament.OLD),
        BookInfo("아모스", "암", 9, Testament.OLD),
        BookInfo("오바댜", "옵", 1, Testament.OLD),
        BookInfo("요나", "욘", 4, Testament.OLD),
        BookInfo("미가", "미", 7, Testament.OLD),
        BookInfo("나훔", "나", 3, Testament.OLD),
        BookInfo("하박국", "합", 3, Testament.OLD),
        BookInfo("스바냐", "습", 3, Testament.OLD),
        BookInfo("학개", "학", 2, Testament.OLD),
        BookInfo("스가랴", "슥", 14, Testament.OLD),
        BookInfo("말라기", "말", 4, Testament.OLD)
    )

    // 신약 27권
    private val newTestament = listOf(
        BookInfo("마태복음", "마", 28, Testament.NEW),
        BookInfo("마가복음", "막", 16, Testament.NEW),
        BookInfo("누가복음", "눅", 24, Testament.NEW),
        BookInfo("요한복음", "요", 21, Testament.NEW),
        BookInfo("사도행전", "행", 28, Testament.NEW),
        BookInfo("로마서", "롬", 16, Testament.NEW),
        BookInfo("고린도전서", "고전", 16, Testament.NEW),
        BookInfo("고린도후서", "고후", 13, Testament.NEW),
        BookInfo("갈라디아서", "갈", 6, Testament.NEW),
        BookInfo("에베소서", "엡", 6, Testament.NEW),
        BookInfo("빌립보서", "빌", 4, Testament.NEW),
        BookInfo("골로새서", "골", 4, Testament.NEW),
        BookInfo("데살로니가전서", "살전", 5, Testament.NEW),
        BookInfo("데살로니가후서", "살후", 3, Testament.NEW),
        BookInfo("디모데전서", "딤전", 6, Testament.NEW),
        BookInfo("디모데후서", "딤후", 4, Testament.NEW),
        BookInfo("디도서", "딛", 3, Testament.NEW),
        BookInfo("빌레몬서", "몬", 1, Testament.NEW),
        BookInfo("히브리서", "히", 13, Testament.NEW),
        BookInfo("야고보서", "약", 5, Testament.NEW),
        BookInfo("베드로전서", "벧전", 5, Testament.NEW),
        BookInfo("베드로후서", "벧후", 3, Testament.NEW),
        BookInfo("요한일서", "요일", 5, Testament.NEW),
        BookInfo("요한이서", "요이", 1, Testament.NEW),
        BookInfo("요한삼서", "요삼", 1, Testament.NEW),
        BookInfo("유다서", "유", 1, Testament.NEW),
        BookInfo("요한계시록", "계", 22, Testament.NEW)
    )

    // 전체 66권
    val allBooks: List<BookInfo> = oldTestament + newTestament

    // 구약만
    val oldTestamentBooks: List<BookInfo> = oldTestament

    // 신약만
    val newTestamentBooks: List<BookInfo> = newTestament

    /**
     * 책 이름으로 BookInfo 찾기
     */
    fun findByName(name: String): BookInfo? {
        return allBooks.find { it.name == name }
    }

    /**
     * 약어로 BookInfo 찾기
     */
    fun findByAbbreviation(abbr: String): BookInfo? {
        return allBooks.find { it.abbreviation == abbr }
    }
}
