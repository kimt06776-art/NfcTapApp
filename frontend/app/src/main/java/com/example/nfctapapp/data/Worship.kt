package com.example.nfctapapp.data

/**
 * 찬양 데이터 모델
 */

data class WorshipSong(
    val id: String,
    val title: String,
    val artist: String,
    val category: WorshipCategory,
    val youtubeVideoId: String,
    val duration: String,
    val lyrics: String? = null,
    val isFavorite: Boolean = false
)

enum class WorshipCategory(val displayName: String, val color: Long) {
    PRAISE("찬양", 0xFF7BC47F),
    CCM("CCM", 0xFFF5A962),
    HYMN("찬송가", 0xFF6B8ED6),
    WORSHIP("워십", 0xFFE87B7B)
}

/**
 * 샘플 데이터 (하드코딩)
 */
object WorshipRepository {

    fun getAllSongs(): List<WorshipSong> {
        return listOf(
            // 워십
            WorshipSong(
                id = "1",
                title = "주 이름 찬양",
                artist = "예수전도단",
                category = WorshipCategory.WORSHIP,
                youtubeVideoId = "pLTGBp-sGEs",
                duration = "5:32",
                isFavorite = true
            ),
            WorshipSong(
                id = "2",
                title = "하나님의 사랑",
                artist = "소망교회",
                category = WorshipCategory.WORSHIP,
                youtubeVideoId = "DZQ3qJdJ0ZQ",
                duration = "4:18"
            ),
            WorshipSong(
                id = "3",
                title = "주의 빛 안에",
                artist = "예수전도단",
                category = WorshipCategory.WORSHIP,
                youtubeVideoId = "kMQvLMZfqrI",
                duration = "6:05"
            ),

            // CCM
            WorshipSong(
                id = "4",
                title = "놀라운 은혜",
                artist = "김남국",
                category = WorshipCategory.CCM,
                youtubeVideoId = "QmUjAUTcRJg",
                duration = "4:42",
                isFavorite = true
            ),
            WorshipSong(
                id = "5",
                title = "주님의 마음",
                artist = "이성은",
                category = WorshipCategory.CCM,
                youtubeVideoId = "3BFH0tW-2Vo",
                duration = "3:58"
            ),
            WorshipSong(
                id = "6",
                title = "주 안에 있는 나에게",
                artist = "온누리워십",
                category = WorshipCategory.CCM,
                youtubeVideoId = "Px8q0w_yJXE",
                duration = "5:15"
            ),

            // 찬송가
            WorshipSong(
                id = "7",
                title = "은혜 아니면",
                artist = "찬송가 405장",
                category = WorshipCategory.HYMN,
                youtubeVideoId = "QEaQiMf_-Fo",
                duration = "3:20"
            ),
            WorshipSong(
                id = "8",
                title = "내 주를 가까이",
                artist = "찬송가 338장",
                category = WorshipCategory.HYMN,
                youtubeVideoId = "cRQPVwn2J9E",
                duration = "2:58",
                isFavorite = true
            ),
            WorshipSong(
                id = "9",
                title = "십자가를 지고",
                artist = "찬송가 150장",
                category = WorshipCategory.HYMN,
                youtubeVideoId = "xHMbJVqWtGE",
                duration = "3:45"
            ),

            // 찬양
            WorshipSong(
                id = "10",
                title = "오직 예수",
                artist = "주향한워십",
                category = WorshipCategory.PRAISE,
                youtubeVideoId = "eImN5CJ_SFs",
                duration = "4:28"
            ),
            WorshipSong(
                id = "11",
                title = "주님 다시 오실 때까지",
                artist = "마커스워십",
                category = WorshipCategory.PRAISE,
                youtubeVideoId = "3fB5_Q6KWmo",
                duration = "5:52"
            ),
            WorshipSong(
                id = "12",
                title = "나의 영혼이",
                artist = "디사이플스",
                category = WorshipCategory.PRAISE,
                youtubeVideoId = "PuvQZq2LqG0",
                duration = "4:05"
            ),

            // 추가 워십
            WorshipSong(
                id = "13",
                title = "예수 이름 높이세",
                artist = "베다니워십",
                category = WorshipCategory.WORSHIP,
                youtubeVideoId = "kGT8sWqKXiE",
                duration = "4:50"
            ),
            WorshipSong(
                id = "14",
                title = "주님 앞에",
                artist = "시온성교회",
                category = WorshipCategory.WORSHIP,
                youtubeVideoId = "vHr6U6pAqoY",
                duration = "3:38"
            ),

            // 추가 CCM
            WorshipSong(
                id = "15",
                title = "주의 이름",
                artist = "위러브",
                category = WorshipCategory.CCM,
                youtubeVideoId = "g0cHrwvkDwE",
                duration = "4:22"
            )
        )
    }

    fun getSongsByCategory(category: WorshipCategory): List<WorshipSong> {
        return getAllSongs().filter { it.category == category }
    }

    fun getFavoriteSongs(): List<WorshipSong> {
        return getAllSongs().filter { it.isFavorite }
    }

    fun getRecommendedSongs(): List<WorshipSong> {
        // 추천 곡: 좋아요 많은 순 또는 최신 순 (하드코딩)
        return getAllSongs().take(5)
    }
}
