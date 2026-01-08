package com.example.nfctapapp.data

/**
 * 교회 공지 데이터 모델
 */

data class Notice(
    val id: String,
    val category: NoticeCategory,
    val title: String,
    val content: String,
    val date: String,
    val author: String,
    val isImportant: Boolean = false
)

enum class NoticeCategory(val displayName: String, val color: Long) {
    WORSHIP("예배", 0xFF6B8ED6),
    EVENT("행사", 0xFFE87B7B),
    RECRUITMENT("모집", 0xFF7BC47F),
    GENERAL("일반", 0xFF9E9E9E)
}

/**
 * 샘플 데이터 (하드코딩)
 */
object NoticeRepository {

    fun getAllNotices(): List<Notice> {
        return listOf(
            Notice(
                id = "1",
                category = NoticeCategory.WORSHIP,
                title = "주일 대예배 안내",
                content = """
                    주일 대예배가 아래와 같이 진행됩니다.

                    📍 장소: 본당 1층
                    ⏰ 시간: 오전 11시
                    🎙️ 설교자: 김목사님

                    모든 성도님들의 많은 참여 부탁드립니다.
                """.trimIndent(),
                date = "2025.01.08",
                author = "교회사무실",
                isImportant = true
            ),
            Notice(
                id = "2",
                category = NoticeCategory.EVENT,
                title = "신년 기도회 안내",
                content = """
                    2025년 신년 기도회를 아래와 같이 개최합니다.

                    📅 일시: 1월 15일 (수) 오후 7시
                    📍 장소: 본당 2층

                    새해를 주님께 의탁하는 귀한 시간이 되기를 바랍니다.
                """.trimIndent(),
                date = "2025.01.07",
                author = "목회부",
                isImportant = true
            ),
            Notice(
                id = "3",
                category = NoticeCategory.RECRUITMENT,
                title = "청년부 찬양팀 모집",
                content = """
                    청년부 찬양팀 신입 멤버를 모집합니다.

                    🎵 모집 분야: 보컬, 기타, 드럼, 키보드
                    📝 신청 방법: 청년부실 방문 또는 전화
                    📞 문의: 010-1234-5678 (김집사)

                    찬양을 사랑하는 청년들의 많은 지원 바랍니다!
                """.trimIndent(),
                date = "2025.01.06",
                author = "청년부"
            ),
            Notice(
                id = "4",
                category = NoticeCategory.EVENT,
                title = "성경통독 마라톤 참가 신청",
                content = """
                    2025년 성경통독 마라톤에 참여하세요!

                    📖 기간: 1월 20일 ~ 12월 31일
                    🎯 목표: 1년 동안 성경 1독 완주
                    🎁 혜택: 완주자 기념품 증정

                    교회 홈페이지에서 신청 가능합니다.
                """.trimIndent(),
                date = "2025.01.05",
                author = "교육부"
            ),
            Notice(
                id = "5",
                category = NoticeCategory.GENERAL,
                title = "주차장 이용 안내",
                content = """
                    주일 예배 시 주차장 이용 안내드립니다.

                    🚗 교회 주차장: 선착순 50대
                    🅿️ 인근 공영주차장: 도보 3분 거리

                    주차 공간이 부족하오니 대중교통 이용을 권장드립니다.
                """.trimIndent(),
                date = "2025.01.04",
                author = "총무부"
            ),
            Notice(
                id = "6",
                category = NoticeCategory.WORSHIP,
                title = "새벽기도회 시간 변경",
                content = """
                    새벽기도회 시간이 아래와 같이 변경되었습니다.

                    변경 전: 오전 5시
                    변경 후: 오전 5시 30분

                    착오 없으시기 바랍니다.
                """.trimIndent(),
                date = "2025.01.03",
                author = "목회부"
            ),
            Notice(
                id = "7",
                category = NoticeCategory.RECRUITMENT,
                title = "주일학교 교사 모집",
                content = """
                    주일학교 교사를 모집합니다.

                    👶 대상: 유아부, 유치부, 초등부
                    📅 기간: 2025년 1월 ~ 12월

                    아이들과 함께 신앙을 나누실 분들의 많은 지원 바랍니다.
                """.trimIndent(),
                date = "2025.01.02",
                author = "교육부"
            )
        )
    }

    fun getNoticesByCategory(category: NoticeCategory): List<Notice> {
        return getAllNotices().filter { it.category == category }
    }

    fun getImportantNotices(): List<Notice> {
        return getAllNotices().filter { it.isImportant }
    }
}
