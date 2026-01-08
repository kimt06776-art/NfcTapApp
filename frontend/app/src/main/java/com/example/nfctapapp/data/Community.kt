package com.example.nfctapapp.data

/**
 * 커뮤니티 데이터 모델
 */

data class CommunityPost(
    val id: String,
    val type: PostType,
    val author: String,
    val authorLevel: Int,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: String,
    val likes: Int,
    val comments: Int,
    val isLiked: Boolean = false
)

enum class PostType(val displayName: String, val emoji: String) {
    PRAYER("기도 제목", "🙏"),
    TESTIMONY("간증", "✨"),
    QUESTION("질문", "❓"),
    SHARING("나눔", "💬")
}

/**
 * 샘플 데이터 (하드코딩)
 */
object CommunityRepository {

    fun getAllPosts(): List<CommunityPost> {
        return listOf(
            CommunityPost(
                id = "1",
                type = PostType.PRAYER,
                author = "김은혜",
                authorLevel = 8,
                content = "내일 중요한 면접이 있습니다. 하나님께서 좋은 결과 주시도록 함께 기도 부탁드립니다. 🙏",
                timestamp = "5분 전",
                likes = 24,
                comments = 8,
                isLiked = false
            ),
            CommunityPost(
                id = "2",
                type = PostType.TESTIMONY,
                author = "이믿음",
                authorLevel = 7,
                content = """
                    오늘 주님의 은혜를 경험했습니다!

                    작년부터 힘든 상황이 계속되었는데, 주님께 모든 것을 맡기고 기도했더니 놀라운 응답을 주셨습니다.

                    "여호와를 기뻐하는 것이 너희의 힘이니라" (느헤미야 8:10)

                    이 말씀이 지금 제 삶의 고백이 되었습니다. 할렐루야! ✨
                """.trimIndent(),
                timestamp = "1시간 전",
                likes = 156,
                comments = 32,
                isLiked = true
            ),
            CommunityPost(
                id = "3",
                type = PostType.QUESTION,
                author = "박사랑",
                authorLevel = 6,
                content = "성경 통독을 시작하려고 하는데, 어떤 순서로 읽는 것이 좋을까요? 선배님들의 조언 부탁드립니다!",
                timestamp = "3시간 전",
                likes = 18,
                comments = 12
            ),
            CommunityPost(
                id = "4",
                type = PostType.PRAYER,
                author = "최소망",
                authorLevel = 6,
                content = "가족의 건강을 위해 기도해주세요. 어머니께서 수술을 앞두고 계십니다. 하나님의 치유하심이 함께하기를 간절히 기도합니다.",
                timestamp = "5시간 전",
                likes = 89,
                comments = 24
            ),
            CommunityPost(
                id = "5",
                type = PostType.SHARING,
                author = "정감사",
                authorLevel = 5,
                content = """
                    오늘 묵상한 말씀을 나눕니다 📖

                    "주께서 내게 이르시되 내 은혜가 네게 족하도다" (고후 12:9)

                    부족함 속에서도 주님의 은혜로 충만하다는 것을 깨달았습니다. 감사합니다!
                """.trimIndent(),
                timestamp = "7시간 전",
                likes = 67,
                comments = 15,
                isLiked = true
            ),
            CommunityPost(
                id = "6",
                type = PostType.TESTIMONY,
                author = "강기쁨",
                authorLevel = 4,
                content = "처음으로 전도해서 친구가 교회에 나오게 되었어요! 너무 기쁩니다. 주님께 영광 돌립니다! 🎉",
                timestamp = "12시간 전",
                likes = 234,
                comments = 45
            ),
            CommunityPost(
                id = "7",
                type = PostType.QUESTION,
                author = "조평화",
                authorLevel = 3,
                content = "새벽기도회에 처음 참석하려고 합니다. 준비해야 할 것이 있을까요?",
                timestamp = "1일 전",
                likes = 12,
                comments = 8
            ),
            CommunityPost(
                id = "8",
                type = PostType.PRAYER,
                author = "윤인내",
                authorLevel = 3,
                content = "취업 준비로 힘든 시간을 보내고 있습니다. 하나님의 인도하심을 구합니다. 🙏",
                timestamp = "1일 전",
                likes = 45,
                comments = 16
            ),
            CommunityPost(
                id = "9",
                type = PostType.SHARING,
                author = "한선행",
                authorLevel = 2,
                content = """
                    오늘 받은 은혜 💕

                    "항상 기뻐하라 쉬지 말고 기도하라 범사에 감사하라" (살전 5:16-18)

                    이 말씀을 실천하려고 노력 중입니다!
                """.trimIndent(),
                timestamp = "2일 전",
                likes = 38,
                comments = 9
            )
        )
    }

    fun getPostsByType(type: PostType): List<CommunityPost> {
        return getAllPosts().filter { it.type == type }
    }
}
