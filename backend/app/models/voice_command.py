"""Voice command models for AI-based voice recognition."""

from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class VoiceCommandAction(str, Enum):
    """Available voice command actions."""
    NAVIGATE_SERMON = "navigate_sermon"
    NAVIGATE_BIBLE = "navigate_bible"
    NAVIGATE_SERMON_NOTE = "navigate_sermon_note"
    NAVIGATE_COMMUNITY = "navigate_community"
    SHOW_DAILY_VERSE = "show_daily_verse"
    PLAY_LATEST_SERMON = "play_latest_sermon"
    UNKNOWN = "unknown"


class VoiceCommandRequest(BaseModel):
    """Request model for voice command analysis."""
    text: str = Field(description="음성에서 변환된 텍스트")
    user_id: Optional[str] = Field(default=None, alias="userId", description="사용자 ID (선택)")

    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "text": "오늘 설교 들려줘",
                "userId": "user-123"
            }
        }


class VoiceCommandAnalysis(BaseModel):
    """AI analysis result of voice command."""
    action: VoiceCommandAction = Field(description="실행할 액션")
    confidence: float = Field(ge=0.0, le=1.0, description="신뢰도 (0.0 ~ 1.0)")
    parameters: Optional[dict] = Field(default=None, description="액션 파라미터")
    message: Optional[str] = Field(default=None, description="사용자에게 보여줄 메시지")

    class Config:
        json_schema_extra = {
            "example": {
                "action": "navigate_sermon",
                "confidence": 0.95,
                "parameters": {"filter": "latest"},
                "message": "최신 설교를 보여드릴게요"
            }
        }


class VoiceCommandResponse(BaseModel):
    """Response model for voice command endpoint."""
    success: bool
    analysis: Optional[VoiceCommandAnalysis] = None
    error: Optional[str] = None
