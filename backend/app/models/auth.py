"""Authentication and NFC registration models."""

from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field
from app.models.user import UserDto


# ==================== NFC Registration DTO ====================

class NfcRegistrationDto(BaseModel):
    """NFC registration model for Supabase 'nfc_registrations' table."""
    id: Optional[str] = None
    user_id: str = Field(alias="userId")
    nfc_uid: str = Field(alias="nfcUid")
    device_id: str = Field(alias="deviceId")
    device_name: Optional[str] = Field(default=None, alias="deviceName")
    is_active: Optional[bool] = Field(default=True, alias="isActive")
    last_used_at: Optional[datetime] = Field(default=None, alias="lastUsedAt")
    created_at: Optional[datetime] = Field(default=None, alias="createdAt")

    class Config:
        from_attributes = True
        populate_by_name = True


# ==================== Auth API Request/Response Models ====================

class NfcAuthRequest(BaseModel):
    """Request model for NFC authentication."""
    nfc_uid: str = Field(alias="nfcUid", description="NFC 태그 UID")
    device_id: str = Field(alias="deviceId", description="디바이스 ID")

    class Config:
        populate_by_name = True


class UserRegisterRequest(BaseModel):
    """Request model for user registration with NFC."""
    name: str = Field(description="사용자 이름")
    phone: Optional[str] = Field(default=None, description="전화번호 (선택)")
    nfc_uid: str = Field(alias="nfcUid", description="NFC 태그 UID")
    device_id: str = Field(alias="deviceId", description="디바이스 ID")
    device_name: Optional[str] = Field(default=None, alias="deviceName", description="디바이스 이름")

    class Config:
        populate_by_name = True


class UserValidateRequest(BaseModel):
    """Request model for validating cached user."""
    user_id: str = Field(alias="userId", description="사용자 ID")
    nfc_uid: str = Field(alias="nfcUid", description="NFC 태그 UID")
    device_id: str = Field(alias="deviceId", description="디바이스 ID")

    class Config:
        populate_by_name = True


class AuthResponse(BaseModel):
    """Response model for authentication operations."""
    success: bool
    user: Optional[UserDto] = None
    error: Optional[str] = None
