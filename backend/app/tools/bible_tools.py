"""
Bible-related tools for the LangChain agent.

These tools allow the agent to search and retrieve Bible verses.
"""

from langchain_core.tools import tool
from typing import Optional
import logging
import json
import os

logger = logging.getLogger(__name__)

# 성경 데이터 로드
_bible_data = None


def _load_bible_data():
    """Load Bible data from JSON file."""
    global _bible_data
    if _bible_data is not None:
        return _bible_data

    try:
        data_path = os.path.join(os.path.dirname(__file__), "..", "data", "john.json")
        with open(data_path, "r", encoding="utf-8") as f:
            _bible_data = json.load(f)
        logger.info(f"Loaded Bible data: {_bible_data['name']}, {_bible_data['totalChapters']} chapters")
    except Exception as e:
        logger.error(f"Failed to load Bible data: {e}")
        _bible_data = {"name": "요한복음", "chapters": []}

    return _bible_data


def _get_verse_text(chapter: int, verse: int) -> Optional[str]:
    """Get a specific verse text."""
    data = _load_bible_data()

    if chapter < 1 or chapter > len(data.get("chapters", [])):
        return None

    chapter_data = data["chapters"][chapter - 1]
    verses = chapter_data.get("verses", [])

    if verse < 1 or verse > len(verses):
        return None

    return verses[verse - 1].get("text")


def _get_chapter_verses(chapter: int) -> list:
    """Get all verses in a chapter."""
    data = _load_bible_data()

    if chapter < 1 or chapter > len(data.get("chapters", [])):
        return []

    chapter_data = data["chapters"][chapter - 1]
    return chapter_data.get("verses", [])


@tool
def bible_search(keyword: str) -> str:
    """성경에서 키워드로 구절을 검색합니다.

    Args:
        keyword: 검색할 키워드 (예: "사랑", "용서", "믿음")

    Returns:
        검색된 성경 구절들의 목록
    """
    logger.info(f"Bible search called with keyword: {keyword}")

    data = _load_bible_data()
    results = []

    for chapter_data in data.get("chapters", []):
        for verse_data in chapter_data.get("verses", []):
            if keyword in verse_data.get("text", ""):
                chapter = verse_data.get("chapter")
                verse = verse_data.get("verse")
                text = verse_data.get("text")
                results.append(f"요한복음 {chapter}:{verse} - {text}")

    if results:
        # 최대 5개만 반환
        if len(results) > 5:
            return "\n\n".join(results[:5]) + f"\n\n... 외 {len(results) - 5}개 구절"
        return "\n\n".join(results)
    else:
        return f"'{keyword}'에 대한 검색 결과가 없습니다."


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

    data = _load_bible_data()
    total_chapters = data.get("totalChapters", 21)

    if chapter < 1 or chapter > total_chapters:
        return f"요한복음은 {total_chapters}장까지 있습니다. (요청: {chapter}장)"

    if verse:
        verse_text = _get_verse_text(chapter, verse)
        if verse_text:
            return f"요한복음 {chapter}장 {verse}절\n\n{verse_text}"
        else:
            chapter_verses = _get_chapter_verses(chapter)
            max_verse = len(chapter_verses)
            return f"요한복음 {chapter}장은 {max_verse}절까지 있습니다. (요청: {verse}절)"
    else:
        # 해당 장의 모든 구절 반환
        chapter_verses = _get_chapter_verses(chapter)
        if chapter_verses:
            verses_text = "\n".join([
                f"{v.get('verse')}절 {v.get('text')}"
                for v in chapter_verses
            ])
            return f"요한복음 {chapter}장\n\n{verses_text}"
        else:
            return f"요한복음 {chapter}장 데이터가 없습니다."
