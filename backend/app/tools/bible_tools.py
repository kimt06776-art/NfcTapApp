"""
Bible-related tools for the LangChain agent.

These tools allow the agent to search and retrieve Bible verses.
"""

from langchain_core.tools import tool
from typing import Optional
import logging

logger = logging.getLogger(__name__)

# 요한복음 데이터 (현재 지원하는 책)
# TODO: 실제 DB에서 가져오도록 변경
JOHN_SAMPLE_VERSES = {
    "3:16": "하나님이 세상을 이처럼 사랑하사 독생자를 주셨으니 이는 그를 믿는 자마다 멸망하지 않고 영생을 얻게 하려 하심이라",
    "14:6": "예수께서 이르시되 내가 곧 길이요 진리요 생명이니 나로 말미암지 않고는 아버지께로 올 자가 없느니라",
    "1:1": "태초에 말씀이 계시니라 이 말씀이 하나님과 함께 계셨으니 이 말씀은 곧 하나님이시니라",
    "11:35": "예수께서 눈물을 흘리시더라",
    "15:13": "사람이 친구를 위하여 자기 목숨을 버리면 이보다 더 큰 사랑이 없나니",
}

# 키워드 기반 검색 인덱스
KEYWORD_INDEX = {
    "사랑": ["3:16", "15:13"],
    "생명": ["3:16", "14:6"],
    "진리": ["14:6"],
    "말씀": ["1:1"],
    "눈물": ["11:35"],
    "친구": ["15:13"],
    "길": ["14:6"],
    "하나님": ["1:1", "3:16"],
    "영생": ["3:16"],
}


@tool
def bible_search(keyword: str) -> str:
    """성경에서 키워드로 구절을 검색합니다.

    Args:
        keyword: 검색할 키워드 (예: "사랑", "용서", "믿음")

    Returns:
        검색된 성경 구절들의 목록
    """
    logger.info(f"Bible search called with keyword: {keyword}")

    results = []

    # 키워드 인덱스에서 검색
    for key, verses in KEYWORD_INDEX.items():
        if keyword in key or key in keyword:
            for verse_ref in verses:
                verse_text = JOHN_SAMPLE_VERSES.get(verse_ref, "")
                if verse_text:
                    results.append(f"요한복음 {verse_ref} - {verse_text}")

    # 직접 텍스트 검색
    if not results:
        for verse_ref, verse_text in JOHN_SAMPLE_VERSES.items():
            if keyword in verse_text:
                results.append(f"요한복음 {verse_ref} - {verse_text}")

    if results:
        return "\n\n".join(results)
    else:
        return f"'{keyword}'에 대한 검색 결과가 없습니다. 현재는 요한복음만 검색 가능합니다."


@tool
def get_verse(book: str, chapter: int, verse: Optional[int] = None) -> str:
    """특정 성경 구절을 가져옵니다.

    Args:
        book: 성경책 이름 (예: "요한복음", "창세기")
        chapter: 장 번호
        verse: 절 번호 (선택사항, 없으면 장 전체)

    Returns:
        성경 구절 내용
    """
    logger.info(f"Get verse called: {book} {chapter}:{verse}")

    if book != "요한복음":
        return f"{book}은(는) 아직 지원하지 않습니다. 현재는 요한복음만 조회 가능합니다."

    if verse:
        verse_ref = f"{chapter}:{verse}"
        verse_text = JOHN_SAMPLE_VERSES.get(verse_ref)
        if verse_text:
            return f"요한복음 {verse_ref}\n{verse_text}"
        else:
            return f"요한복음 {verse_ref} 구절을 찾을 수 없습니다."
    else:
        # 해당 장의 모든 구절 반환
        chapter_verses = []
        for verse_ref, verse_text in JOHN_SAMPLE_VERSES.items():
            if verse_ref.startswith(f"{chapter}:"):
                chapter_verses.append(f"{verse_ref} {verse_text}")

        if chapter_verses:
            return f"요한복음 {chapter}장\n" + "\n".join(chapter_verses)
        else:
            return f"요한복음 {chapter}장 데이터가 없습니다."
