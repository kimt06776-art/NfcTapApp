"""User models."""

from typing import Optional
from datetime import datetime
from pydantic import BaseModel


class UserDto(BaseModel):
    """User model for Supabase 'users' table."""
    id: Optional[str] = None
    name: str
    phone: Optional[str] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True
