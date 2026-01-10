"""
LangChain Tools for NfcTapApp Agent.

Tools enable the agent to perform actions like:
- Searching Bible verses
- Getting sermon information
- Navigating to app screens
"""

from app.tools.bible_tools import bible_search, get_verse
from app.tools.navigation_tools import navigate_to_screen, get_available_screens

__all__ = [
    "bible_search",
    "get_verse",
    "navigate_to_screen",
    "get_available_screens",
]

# 모든 도구를 리스트로 제공
ALL_TOOLS = [
    bible_search,
    get_verse,
    navigate_to_screen,
    get_available_screens,
]
