package com.example.nfctapapp.data

/**
 * 게이미피케이션 데이터 모델
 */

data class UserLevel(
    val level: Int,
    val title: String,
    val currentPoints: Int,
    val nextLevelPoints: Int,
    val progress: Float // 0.0 ~ 1.0
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // 이모지
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)

data class ActivityStats(
    val totalSermons: Int,
    val totalBibleReading: Int,
    val totalPrayers: Int,
    val totalDays: Int,
    val streak: Int // 연속 출석 일수
)

/**
 * 샘플 데이터 (하드코딩)
 */
object GamificationRepository {

    fun getUserLevel(): UserLevel {
        return UserLevel(
            level = 5,
            title = "믿음의 전사",
            currentPoints = 1250,
            nextLevelPoints = 1500,
            progress = 0.83f
        )
    }

    fun getAllBadges(): List<Badge> {
        return listOf(
            Badge(
                id = "first_sermon",
                name = "첫 설교",
                description = "첫 번째 설교를 들었습니다",
                icon = "🎙️",
                isUnlocked = true,
                unlockedDate = "2025.01.01"
            ),
            Badge(
                id = "bible_reader",
                name = "성경 독자",
                description = "성경을 10번 읽었습니다",
                icon = "📖",
                isUnlocked = true,
                unlockedDate = "2025.01.03"
            ),
            Badge(
                id = "prayer_warrior",
                name = "기도의 용사",
                description = "기도를 50회 완료했습니다",
                icon = "🙏",
                isUnlocked = true,
                unlockedDate = "2025.01.05"
            ),
            Badge(
                id = "week_streak",
                name = "7일 연속",
                description = "7일 연속 출석했습니다",
                icon = "🔥",
                isUnlocked = true,
                unlockedDate = "2025.01.07"
            ),
            Badge(
                id = "community_active",
                name = "커뮤니티 활동가",
                description = "커뮤니티에 10개 글을 작성했습니다",
                icon = "💬",
                isUnlocked = true,
                unlockedDate = "2025.01.06"
            ),
            Badge(
                id = "worship_lover",
                name = "찬양 애호가",
                description = "찬양을 20곡 들었습니다",
                icon = "🎵",
                isUnlocked = true,
                unlockedDate = "2025.01.04"
            ),
            Badge(
                id = "early_bird",
                name = "새벽기도",
                description = "새벽 5시에 접속했습니다",
                icon = "🌅",
                isUnlocked = true,
                unlockedDate = "2025.01.02"
            ),
            Badge(
                id = "sharing_love",
                name = "나눔의 사랑",
                description = "5명에게 앱을 추천했습니다",
                icon = "❤️",
                isUnlocked = true,
                unlockedDate = "2025.01.05"
            ),
            Badge(
                id = "month_streak",
                name = "30일 연속",
                description = "30일 연속 출석하세요",
                icon = "🏆",
                isUnlocked = false
            ),
            Badge(
                id = "sermon_master",
                name = "설교 마스터",
                description = "100개의 설교를 들으세요",
                icon = "🎓",
                isUnlocked = false
            ),
            Badge(
                id = "bible_expert",
                name = "성경 전문가",
                description = "성경 66권을 모두 읽으세요",
                icon = "📚",
                isUnlocked = false
            ),
            Badge(
                id = "prayer_master",
                name = "기도의 달인",
                description = "기도를 1000회 완료하세요",
                icon = "✨",
                isUnlocked = false
            )
        )
    }

    fun getActivityStats(): ActivityStats {
        return ActivityStats(
            totalSermons = 24,
            totalBibleReading = 47,
            totalPrayers = 156,
            totalDays = 18,
            streak = 7
        )
    }

    fun getLeaderboard(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(rank = 1, name = "김은혜", points = 3520, level = 8),
            LeaderboardEntry(rank = 2, name = "이믿음", points = 2890, level = 7),
            LeaderboardEntry(rank = 3, name = "박사랑", points = 2450, level = 6),
            LeaderboardEntry(rank = 4, name = "최소망", points = 1980, level = 6),
            LeaderboardEntry(rank = 5, name = "나", points = 1250, level = 5, isCurrentUser = true),
            LeaderboardEntry(rank = 6, name = "정감사", points = 1120, level = 5),
            LeaderboardEntry(rank = 7, name = "강기쁨", points = 890, level = 4),
            LeaderboardEntry(rank = 8, name = "조평화", points = 650, level = 3),
            LeaderboardEntry(rank = 9, name = "윤인내", points = 420, level = 3),
            LeaderboardEntry(rank = 10, name = "한선행", points = 280, level = 2)
        )
    }
}

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val points: Int,
    val level: Int,
    val isCurrentUser: Boolean = false
)
