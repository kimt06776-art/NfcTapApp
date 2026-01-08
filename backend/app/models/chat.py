"""Chat session and message models."""

from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field


# ==================== Chat DTOs ====================

class ChatSessionDto(BaseModel):
    """Chat session model for Supabase 'chat_sessions' table."""
    id: Optional[str] = None
    user_id: str = Field(alias="userId")
    title: Optional[str] = None
    created_at: Optional[datetime] = Field(default=None, alias="createdAt")
    updated_at: Optional[datetime] = Field(default=None, alias="updatedAt")

    class Config:
        from_attributes = True
        populate_by_name = True


class ChatMessageDto(BaseModel):
    """Chat message model for Supabase 'chat_messages' table."""
    id: Optional[str] = None
    session_id: str = Field(alias="sessionId")
    content: str
    is_from_user: bool = Field(alias="isFromUser")
    created_at: Optional[datetime] = Field(default=None, alias="createdAt")

    class Config:
        from_attributes = True
        populate_by_name = True


class ChatSessionInsert(BaseModel):
    """Insert model for creating new chat session."""
    user_id: str = Field(alias="userId")
    title: Optional[str] = None

    class Config:
        populate_by_name = True


class ChatMessageInsert(BaseModel):
    """Insert model for creating new chat message."""
    session_id: str = Field(alias="sessionId")
    content: str
    is_from_user: bool = Field(alias="isFromUser")

    class Config:
        populate_by_name = True


# ==================== Chat API Request/Response Models ====================

class ChatStreamRequest(BaseModel):
    """Request model for chat streaming endpoint."""
    session_id: str = Field(alias="sessionId", description="채팅 세션 ID")
    user_message: str = Field(alias="userMessage", description="사용자 메시지")

    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "sessionId": "session-uuid-123",
                "userMessage": "오늘 기도에 대해 고민이 있어요"
            }
        }


class SessionCreateRequest(BaseModel):
    """Request model for creating a new chat session."""
    user_id: str = Field(alias="userId", description="사용자 ID")
    title: Optional[str] = Field(default=None, description="세션 제목 (선택)")

    class Config:
        populate_by_name = True


class SessionUpdateRequest(BaseModel):
    """Request model for updating session title."""
    title: str = Field(description="새 세션 제목")

    class Config:
        json_schema_extra = {
            "example": {
                "title": "기도에 대한 고민"
            }
        }


class SessionListResponse(BaseModel):
    """Response model for session list."""
    success: bool
    sessions: Optional[list[ChatSessionDto]] = None
    error: Optional[str] = None


class MessageListResponse(BaseModel):
    """Response model for message list."""
    success: bool
    messages: Optional[list[ChatMessageDto]] = None
    error: Optional[str] = None


class SessionCreateResponse(BaseModel):
    """Response model for session creation."""
    success: bool
    session: Optional[ChatSessionDto] = None
    error: Optional[str] = None


class MessageCreateResponse(BaseModel):
    """Response model for message creation."""
    success: bool
    message: Optional[ChatMessageDto] = None
    error: Optional[str] = None
