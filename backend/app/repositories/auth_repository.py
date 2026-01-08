"""
Auth repository for NFC authentication and user management.

Migrated from frontend AuthRepositoryImpl.kt to backend for security.
"""

import logging
from typing import Optional
from app.models import UserDto, NfcRegistrationDto
from app.repositories.supabase_client import get_supabase_client

logger = logging.getLogger(__name__)


class AuthRepository:
    """Repository for authentication and NFC registration."""

    def __init__(self):
        """Initialize repository with Supabase client."""
        self.supabase = get_supabase_client()

    async def authenticate_with_nfc(
        self,
        nfc_uid: str,
        device_id: str
    ) -> Optional[UserDto]:
        """
        Authenticate user with NFC tag and device.

        Args:
            nfc_uid: NFC tag UID
            device_id: Device ID

        Returns:
            UserDto if authenticated, None if not registered

        Raises:
            Exception: If authentication fails
        """
        try:
            # Query nfc_registrations with user join
            response = self.supabase.table("nfc_registrations") \
                .select("*, users(*)") \
                .eq("nfc_uid", nfc_uid) \
                .eq("device_id", device_id) \
                .eq("is_active", True) \
                .execute()

            if not response.data:
                return None

            registration = response.data[0]
            user_data = registration.get("users")

            if not user_data:
                logger.warning(f"Registration exists but no user found: {registration.get('id')}")
                return None

            # Update last_used_at
            self.supabase.table("nfc_registrations") \
                .update({"last_used_at": "now()"}) \
                .eq("id", registration.get("id")) \
                .execute()

            return UserDto(
                id=user_data.get("id"),
                name=user_data.get("name"),
                phone=user_data.get("phone"),
                created_at=user_data.get("created_at")
            )

        except Exception as e:
            logger.error(f"NFC authentication failed: {str(e)}", exc_info=True)
            raise

    async def register_user(
        self,
        name: str,
        phone: Optional[str],
        nfc_uid: str,
        device_id: str,
        device_name: Optional[str]
    ) -> UserDto:
        """
        Register a new user with NFC tag.

        Args:
            name: User name
            phone: User phone (optional)
            nfc_uid: NFC tag UID
            device_id: Device ID
            device_name: Device name (optional)

        Returns:
            UserDto: Created user

        Raises:
            Exception: If registration fails
        """
        try:
            # 1. Create user
            user_data = {
                "name": name,
                "phone": phone
            }

            user_response = self.supabase.table("users").insert(user_data).execute()

            if not user_response.data:
                raise Exception("Failed to create user")

            created_user = user_response.data[0]
            user_id = created_user.get("id")

            # 2. Create NFC registration
            registration_data = {
                "user_id": user_id,
                "nfc_uid": nfc_uid,
                "device_id": device_id,
                "device_name": device_name
            }

            self.supabase.table("nfc_registrations").insert(registration_data).execute()

            return UserDto(
                id=user_id,
                name=created_user.get("name"),
                phone=created_user.get("phone"),
                created_at=created_user.get("created_at")
            )

        except Exception as e:
            logger.error(f"User registration failed: {str(e)}", exc_info=True)
            raise

    async def validate_user(
        self,
        user_id: str,
        nfc_uid: str,
        device_id: str
    ) -> Optional[UserDto]:
        """
        Validate cached user credentials.

        Args:
            user_id: User ID from cache
            nfc_uid: NFC tag UID
            device_id: Device ID

        Returns:
            UserDto if valid, None if invalid

        Raises:
            Exception: If validation fails
        """
        try:
            # Verify registration exists and is active
            response = self.supabase.table("nfc_registrations") \
                .select("*, users(*)") \
                .eq("user_id", user_id) \
                .eq("nfc_uid", nfc_uid) \
                .eq("device_id", device_id) \
                .eq("is_active", True) \
                .execute()

            if not response.data:
                return None

            registration = response.data[0]
            user_data = registration.get("users")

            if not user_data:
                return None

            return UserDto(
                id=user_data.get("id"),
                name=user_data.get("name"),
                phone=user_data.get("phone"),
                created_at=user_data.get("created_at")
            )

        except Exception as e:
            logger.error(f"User validation failed: {str(e)}", exc_info=True)
            raise


# Global repository instance
auth_repository = AuthRepository()
