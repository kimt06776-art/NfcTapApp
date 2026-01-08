"""Pathway suggestion models."""

from typing import Optional, Literal
from pydantic import BaseModel, Field


# ==================== Pathway Response Models ====================

class BibleVerse(BaseModel):
    """Bible verse reference and text."""
    book: str = Field(description="성경책 이름 (예: 시편, 요한복음)")
    chapter: int = Field(description="장 번호")
    verse: str = Field(description="절 번호 (예: '1' or '1-3')")
    text: str = Field(description="개역개정 성경 본문")


class PathwayResponse(BaseModel):
    """
    LLM structured output for pathway suggestion.

    Based on docs/design/02-json-schema.md
    """
    pathway: Literal["prayer", "word", "silence"] = Field(
        description="선택된 길: prayer(기도), word(말씀), silence(침묵)"
    )

    verse: Optional[BibleVerse] = Field(
        default=None,
        description="pathway가 'word'일 때만 필수"
    )

    prayer_text: Optional[str] = Field(
        default=None,
        max_length=200,
        description="pathway가 'prayer'일 때만 필수. 짧은 기도문 (200자 이내)"
    )

    silence_guide: Optional[str] = Field(
        default=None,
        max_length=100,
        description="pathway가 'silence'일 때만 필수. 침묵 가이드 (100자 이내)"
    )

    brief_connection: str = Field(
        max_length=50,
        description="사용자 입력과 pathway를 연결하는 짧은 문구 (50자 이내)"
    )

    crisis_detected: bool = Field(
        default=False,
        description="위기 상황 감지 여부 (자해, 자살, 위해 등)"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "pathway": "word",
                "verse": {
                    "book": "시편",
                    "chapter": 23,
                    "verse": "1",
                    "text": "여호와는 나의 목자시니 내게 부족함이 없으리로다"
                },
                "prayer_text": None,
                "silence_guide": None,
                "brief_connection": "오늘 하루도 주님께서 함께하십니다.",
                "crisis_detected": False
            }
        }


# ==================== Pathway API Request/Response ====================

class PathwayRequest(BaseModel):
    """Request model for /api/pathway endpoint."""
    user_id: str = Field(description="사용자 ID")
    session_id: Optional[str] = Field(default=None, description="채팅 세션 ID (선택)")
    user_input: str = Field(description="사용자 입력 텍스트")

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": "user-uuid-123",
                "session_id": "session-uuid-456",
                "user_input": "오늘 너무 힘들었어요"
            }
        }


class PathwayApiResponse(BaseModel):
    """API response wrapping PathwayResponse."""
    success: bool = Field(description="요청 성공 여부")
    data: Optional[PathwayResponse] = Field(default=None, description="Pathway 응답 데이터")
    error: Optional[str] = Field(default=None, description="에러 메시지")
    message_id: Optional[str] = Field(default=None, description="저장된 메시지 ID")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "data": {
                    "pathway": "word",
                    "verse": {
                        "book": "시편",
                        "chapter": 23,
                        "verse": "1",
                        "text": "여호와는 나의 목자시니 내게 부족함이 없으리로다"
                    },
                    "brief_connection": "오늘 하루도 주님께서 함께하십니다.",
                    "crisis_detected": False
                },
                "error": None,
                "message_id": "msg-uuid-789"
            }
        }
