"""
Navigation tools for the LangChain agent.

These tools allow the agent to trigger app navigation.
"""

from langchain_core.tools import tool
from typing import Literal
import logging

logger = logging.getLogger(__name__)

# 지원하는 화면 목록
SUPPORTED_SCREENS = {
    "bible": "성경 읽기 화면",
    "sermon_note": "설교 노트 화면",
    "today_verse": "오늘의 말씀 화면",
    "sermon": "설교 화면",
    "community": "커뮤니티 화면",
    "gamification": "게이미피케이션 화면",
    "worship": "예배 화면",
}


@tool
def navigate_to_screen(
    screen: Literal["bible", "sermon_note", "today_verse", "sermon", "community", "gamification", "worship"],
    params: str = ""
) -> str:
    """앱의 특정 화면으로 이동합니다.

    Args:
        screen: 이동할 화면 이름
            - "bible": 성경 읽기 화면
            - "sermon_note": 설교 노트 화면
            - "today_verse": 오늘의 말씀 화면
            - "sermon": 설교 화면
            - "community": 커뮤니티 화면
            - "gamification": 게이미피케이션 화면
            - "worship": 예배 화면
        params: 추가 파라미터 (예: "chapter=3&verse=16")

    Returns:
        네비게이션 결과 메시지
    """
    logger.info(f"Navigate to screen called: {screen} with params: {params}")

    if screen not in SUPPORTED_SCREENS:
        return f"'{screen}'은(는) 지원하지 않는 화면입니다. 지원 화면: {', '.join(SUPPORTED_SCREENS.keys())}"

    screen_name = SUPPORTED_SCREENS[screen]

    # 특별 처리가 필요한 화면들
    if screen == "bible" and params:
        return f"[NAVIGATE:bible?{params}] {screen_name}으로 이동합니다."
    else:
        return f"[NAVIGATE:{screen}] {screen_name}으로 이동합니다."


@tool
def get_available_screens() -> str:
    """이동 가능한 화면 목록을 반환합니다.

    Returns:
        사용 가능한 화면 목록
    """
    screens_list = "\n".join([f"- {key}: {value}" for key, value in SUPPORTED_SCREENS.items()])
    return f"이동 가능한 화면 목록:\n{screens_list}"
