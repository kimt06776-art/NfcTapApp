"""Sermon note models."""

from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field


# ==================== Sermon Note DTOs ====================

class SermonNoteDto(BaseModel):
    """Sermon note model for Supabase 'sermon_notes' table."""
    id: Optional[str] = None
    user_id: str = Field(alias="userId")
    title: str
    content: str
    scripture: Optional[str] = None
    created_at: Optional[datetime] = Field(default=None, alias="createdAt")
    updated_at: Optional[datetime] = Field(default=None, alias="updatedAt")

    class Config:
        from_attributes = True
        populate_by_name = True


# ==================== Request/Response Models ====================

class SermonNoteCreateRequest(BaseModel):
    """Request model for creating a sermon note."""
    user_id: str = Field(alias="userId", description="사용자 ID")
    title: str = Field(description="설교 제목")
    content: str = Field(description="노트 내용")
    scripture: Optional[str] = Field(default=None, description="본문 말씀")

    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "userId": "user-uuid-123",
                "title": "믿음으로 살아가기",
                "content": "히브리서 11장 말씀을 통해...",
                "scripture": "히브리서 11:1-6"
            }
        }


class SermonNoteUpdateRequest(BaseModel):
    """Request model for updating a sermon note."""
    title: Optional[str] = Field(default=None, description="설교 제목")
    content: Optional[str] = Field(default=None, description="노트 내용")
    scripture: Optional[str] = Field(default=None, description="본문 말씀")

    class Config:
        json_schema_extra = {
            "example": {
                "title": "수정된 설교 제목",
                "content": "수정된 내용..."
            }
        }


class SermonNoteListResponse(BaseModel):
    """Response model for sermon note list."""
    success: bool
    notes: Optional[list[SermonNoteDto]] = None
    error: Optional[str] = None


class SermonNoteResponse(BaseModel):
    """Response model for single sermon note."""
    success: bool
    note: Optional[SermonNoteDto] = None
    error: Optional[str] = None


class SermonNoteDeleteResponse(BaseModel):
    """Response model for sermon note deletion."""
    success: bool
    message: Optional[str] = None
    error: Optional[str] = None
