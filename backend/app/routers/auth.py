"""
FastAPI router for authentication and NFC operations.

Includes:
- NFC authentication
- User registration
- User validation
"""

import logging
from fastapi import APIRouter, HTTPException
from app.models import (
    NfcAuthRequest,
    UserRegisterRequest,
    UserValidateRequest,
    AuthResponse
)
from app.repositories.auth_repository import auth_repository

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post("/auth/nfc", response_model=AuthResponse)
async def authenticate_with_nfc(request: NfcAuthRequest):
    """
    Authenticate user with NFC tag and device.

    Migrated from frontend AuthRepositoryImpl.kt for security.

    Args:
        request: NfcAuthRequest with nfcUid and deviceId

    Returns:
        AuthResponse: User if authenticated, error if not registered
    """
    try:
        user = await auth_repository.authenticate_with_nfc(
            nfc_uid=request.nfc_uid,
            device_id=request.device_id
        )

        if user is None:
            return AuthResponse(
                success=False,
                user=None,
                error="NFC 태그가 등록되지 않았습니다"
            )

        return AuthResponse(
            success=True,
            user=user,
            error=None
        )

    except Exception as e:
        logger.error(f"NFC authentication failed: {str(e)}", exc_info=True)
        return AuthResponse(
            success=False,
            user=None,
            error=str(e)
        )


@router.post("/auth/register", response_model=AuthResponse)
async def register_user(request: UserRegisterRequest):
    """
    Register a new user with NFC tag.

    Args:
        request: UserRegisterRequest with name, phone, nfcUid, deviceId, deviceName

    Returns:
        AuthResponse: Created user
    """
    try:
        user = await auth_repository.register_user(
            name=request.name,
            phone=request.phone,
            nfc_uid=request.nfc_uid,
            device_id=request.device_id,
            device_name=request.device_name
        )

        return AuthResponse(
            success=True,
            user=user,
            error=None
        )

    except Exception as e:
        logger.error(f"User registration failed: {str(e)}", exc_info=True)
        return AuthResponse(
            success=False,
            user=None,
            error=str(e)
        )


@router.post("/auth/validate", response_model=AuthResponse)
async def validate_user(request: UserValidateRequest):
    """
    Validate cached user credentials.

    Args:
        request: UserValidateRequest with userId, nfcUid, deviceId

    Returns:
        AuthResponse: User if valid, error if invalid
    """
    try:
        user = await auth_repository.validate_user(
            user_id=request.user_id,
            nfc_uid=request.nfc_uid,
            device_id=request.device_id
        )

        if user is None:
            return AuthResponse(
                success=False,
                user=None,
                error="유효하지 않은 사용자 정보입니다"
            )

        return AuthResponse(
            success=True,
            user=user,
            error=None
        )

    except Exception as e:
        logger.error(f"User validation failed: {str(e)}", exc_info=True)
        return AuthResponse(
            success=False,
            user=None,
            error=str(e)
        )
