package com.example.nfctapapp.data

/**
 * 묵상 콘텐츠 데이터 모델
 */

data class MeditationContent(
    val id: String,
    val title: String,
    val author: String,
    val date: String,
    val previewText: String,
    val fullText: String,
    val bibleReference: String
)

data class BibleRecommendation(
    val book: String,
    val chapter: Int,
    val verses: String,
    val theme: String,
    val description: String
)

data class WorshipRecommendation(
    val songId: String,
    val title: String,
    val artist: String,
    val reason: String
)

/**
 * 샘플 데이터 (하드코딩)
 */
object MeditationRepository {

    fun getTodayMeditation(): MeditationContent {
        return MeditationContent(
            id = "1",
            title = "주님의 인도하심을 따라",
            author = "이영훈 목사",
            date = "2024.01.15",
            previewText = "우리는 때때로 삶의 방향을 잃고 헤맬 때가 있습니다. 하지만 주님은 언제나 우리를 올바른 길로 인도하십니다...",
            fullText = """
                우리는 때때로 삶의 방향을 잃고 헤맬 때가 있습니다. 하지만 주님은 언제나 우리를 올바른 길로 인도하십니다.

                시편 23편은 우리에게 이렇게 말씀하십니다. "여호와는 나의 목자시니 내게 부족함이 없으리로다"

                오늘 하루도 주님의 인도하심을 신뢰하며, 그분이 예비하신 길을 담대히 걸어갑시다.
                우리의 목자이신 주님께서 우리를 푸른 초장과 쉴 만한 물가로 인도하실 것입니다.
            """.trimIndent(),
            bibleReference = "시편 23:1-3"
        )
    }

    fun getBibleRecommendation(): BibleRecommendation {
        return BibleRecommendation(
            book = "요한복음",
            chapter = 15,
            verses = "1-8",
            theme = "포도나무와 가지",
            description = "그리스도와 연합된 삶을 통해 풍성한 열매를 맺는 비결을 배웁니다."
        )
    }

    fun getWorshipRecommendation(): WorshipRecommendation {
        return WorshipRecommendation(
            songId = "1",
            title = "주 이름 찬양",
            artist = "예수전도단",
            reason = "오늘의 묵상 주제와 함께 부르기 좋은 찬양입니다."
        )
    }
}
