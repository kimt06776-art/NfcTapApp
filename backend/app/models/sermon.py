"""Sermon models."""

from typing import Optional
from pydantic import BaseModel, Field
from datetime import datetime


# ==================== Sermon Database Model ====================

class SermonDto(BaseModel):
    """Sermon data from database."""
    id: str
    youtube_video_id: str
    title: str
    preacher: Optional[str] = None
    description: Optional[str] = None
    scripture: Optional[str] = None
    thumbnail_url: Optional[str] = None
    published_at: str  # ISO datetime string
    is_active: bool = True


# ==================== Sermon API Response Models ====================

class SermonListResponse(BaseModel):
    """Response for sermon list API."""
    success: bool
    sermons: Optional[list[SermonDto]] = None
    error: Optional[str] = None

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "sermons": [
                    {
                        "id": "sermon-uuid-123",
                        "youtube_video_id": "dQw4w9WgXcQ",
                        "title": "은혜로운 설교",
                        "preacher": "김목사",
                        "description": "설교 설명",
                        "scripture": "요한복음 3:16",
                        "thumbnail_url": "https://example.com/thumb.jpg",
                        "published_at": "2026-01-07T00:00:00Z",
                        "is_active": True
                    }
                ],
                "error": None
            }
        }


class SermonDetailResponse(BaseModel):
    """Response for single sermon API."""
    success: bool
    sermon: Optional[SermonDto] = None
    error: Optional[str] = None

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "sermon": {
                    "id": "sermon-uuid-123",
                    "youtube_video_id": "dQw4w9WgXcQ",
                    "title": "은혜로운 설교",
                    "preacher": "김목사",
                    "description": "설교 설명",
                    "scripture": "요한복음 3:16",
                    "thumbnail_url": "https://example.com/thumb.jpg",
                    "published_at": "2026-01-07T00:00:00Z",
                    "is_active": True
                },
                "error": None
            }
        }
