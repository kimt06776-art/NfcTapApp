"""
Intent classification models for chat routing.

Used by the router to classify user messages and route to appropriate agents.
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class IntentType(str, Enum):
    """Types of user intent for chat routing."""
    BIBLE_STUDY = "bible_study"      # 성경 공부, 해석, 의미 질문
    BIBLE_READING = "bible_reading"  # 성경 구절 찾기/읽기 요청
    COUNSELING = "counseling"        # 위로, 상담, 고민 상담
    PRAYER = "prayer"                # 기도 요청, 기도문 작성
    GENERAL = "general"              # 일반 대화, 인사, 기타


class IntentClassification(BaseModel):
    """LLM structured output for intent classification."""
    intent: IntentType = Field(
        description="분류된 사용자 의도"
    )
    confidence: float = Field(
        ge=0.0, le=1.0,
        description="분류 확신도 (0.0 ~ 1.0)"
    )
    reasoning: str = Field(
        description="분류 근거 (간단히)"
    )


class BibleStudyResponse(BaseModel):
    """Structured response for Bible study questions."""
    passage_explanation: str = Field(
        description="본문 해설 - 구절의 의미와 맥락 설명"
    )
    historical_context: str = Field(
        description="역사적 배경 - 당시 시대적, 문화적 배경"
    )
    key_words: list[str] = Field(
        description="핵심 단어 - 중요한 단어들과 그 의미"
    )
    life_application: str = Field(
        description="삶의 적용 - 오늘날 우리 삶에 어떻게 적용할 수 있는지"
    )
    reflection_questions: list[str] = Field(
        description="묵상 질문 - 깊이 생각해볼 질문들 (2-3개)"
    )
    related_verses: list[str] = Field(
        description="관련 구절 - 함께 읽으면 좋은 성경 구절들"
    )


class BibleReadingResponse(BaseModel):
    """Parsed response for Bible reading requests."""
    book: str = Field(
        description="성경책 이름 (예: 요한복음, 창세기)"
    )
    chapter: Optional[int] = Field(
        default=None,
        description="장 번호 (파싱 실패 시 None)"
    )
    verse: Optional[int] = Field(
        default=None,
        description="절 번호 (없으면 장 전체)"
    )
    verse_end: Optional[int] = Field(
        default=None,
        description="끝 절 번호 (범위 지정 시)"
    )
    is_supported: bool = Field(
        default=True,
        description="지원되는 성경책인지 여부"
    )
    error_message: Optional[str] = Field(
        default=None,
        description="오류 메시지 (지원 안 되는 경우)"
    )


class ChatRouterResponse(BaseModel):
    """Response wrapper that includes intent and response type."""
    intent: IntentType
    response_type: str = Field(
        description="응답 타입: 'stream' | 'structured' | 'navigation'"
    )
    # For structured responses (bible_study)
    structured_response: Optional[BibleStudyResponse] = None
    # For navigation responses (bible_reading)
    navigation_response: Optional[BibleReadingResponse] = None
    # For stream responses, content will be streamed separately
