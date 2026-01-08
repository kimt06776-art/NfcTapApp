"""
Chat repository for Supabase operations.

Migrated from frontend ChatRepository.kt to backend for security.
"""

import logging
from typing import Optional
from datetime import datetime
from app.models import (
    ChatSessionDto,
    ChatMessageDto,
    ChatSessionInsert,
    ChatMessageInsert
)
from app.repositories.supabase_client import get_supabase_client

logger = logging.getLogger(__name__)


class ChatRepository:
    """Repository for chat sessions and messages."""

    def __init__(self):
        """Initialize repository with Supabase client."""
        self.supabase = get_supabase_client()

    # ==================== Session Operations ====================

    async def create_session(self, user_id: str, title: Optional[str] = None) -> ChatSessionDto:
        """
        Create a new chat session.

        Args:
            user_id: User ID
            title: Optional session title

        Returns:
            ChatSessionDto: Created session

        Raises:
            Exception: If creation fails
        """
        try:
            data = {
                "user_id": user_id,
                "title": title
            }

            response = self.supabase.table("chat_sessions").insert(data).execute()

            if not response.data:
                raise Exception("Failed to create session")

            session_data = response.data[0]

            return ChatSessionDto(
                id=session_data.get("id"),
                userId=session_data.get("user_id"),
                title=session_data.get("title"),
                createdAt=session_data.get("created_at"),
                updatedAt=session_data.get("updated_at")
            )

        except Exception as e:
            logger.error(f"Failed to create session: {str(e)}", exc_info=True)
            raise

    async def get_sessions(self, user_id: str) -> list[ChatSessionDto]:
        """
        Get all sessions for a user.

        Args:
            user_id: User ID

        Returns:
            list[ChatSessionDto]: List of sessions ordered by updated_at desc
        """
        try:
            response = self.supabase.table("chat_sessions") \
                .select("*") \
                .eq("user_id", user_id) \
                .order("updated_at", desc=True) \
                .execute()

            sessions = []
            for session_data in response.data:
                sessions.append(ChatSessionDto(
                    id=session_data.get("id"),
                    userId=session_data.get("user_id"),
                    title=session_data.get("title"),
                    createdAt=session_data.get("created_at"),
                    updatedAt=session_data.get("updated_at")
                ))

            return sessions

        except Exception as e:
            logger.error(f"Failed to get sessions: {str(e)}", exc_info=True)
            raise

    async def update_session_title(self, session_id: str, title: str) -> None:
        """
        Update session title.

        Args:
            session_id: Session ID
            title: New title

        Raises:
            Exception: If update fails
        """
        try:
            self.supabase.table("chat_sessions") \
                .update({"title": title}) \
                .eq("id", session_id) \
                .execute()

        except Exception as e:
            logger.error(f"Failed to update session title: {str(e)}", exc_info=True)
            raise

    async def delete_session(self, session_id: str) -> None:
        """
        Delete a session (cascade deletes messages).

        Args:
            session_id: Session ID

        Raises:
            Exception: If deletion fails
        """
        try:
            self.supabase.table("chat_sessions") \
                .delete() \
                .eq("id", session_id) \
                .execute()

        except Exception as e:
            logger.error(f"Failed to delete session: {str(e)}", exc_info=True)
            raise

    # ==================== Message Operations ====================

    async def get_messages(self, session_id: str) -> list[ChatMessageDto]:
        """
        Get all messages for a session.

        Args:
            session_id: Session ID

        Returns:
            list[ChatMessageDto]: List of messages ordered by created_at asc
        """
        try:
            response = self.supabase.table("chat_messages") \
                .select("*") \
                .eq("session_id", session_id) \
                .order("created_at", desc=False) \
                .execute()

            messages = []
            for msg_data in response.data:
                messages.append(ChatMessageDto(
                    id=msg_data.get("id"),
                    sessionId=msg_data.get("session_id"),
                    content=msg_data.get("content"),
                    isFromUser=msg_data.get("is_from_user"),
                    createdAt=msg_data.get("created_at")
                ))

            return messages

        except Exception as e:
            logger.error(f"Failed to get messages: {str(e)}", exc_info=True)
            raise

    async def save_message(
        self,
        session_id: str,
        content: str,
        is_from_user: bool
    ) -> ChatMessageDto:
        """
        Save a new message.

        Args:
            session_id: Session ID
            content: Message content
            is_from_user: True if from user, False if from AI

        Returns:
            ChatMessageDto: Created message

        Raises:
            Exception: If save fails
        """
        try:
            # Save message
            data = {
                "session_id": session_id,
                "content": content,
                "is_from_user": is_from_user
            }

            response = self.supabase.table("chat_messages").insert(data).execute()

            if not response.data:
                raise Exception("Failed to save message")

            msg_data = response.data[0]

            # Update session's updated_at timestamp
            self.supabase.table("chat_sessions") \
                .update({"updated_at": "now()"}) \
                .eq("id", session_id) \
                .execute()

            return ChatMessageDto(
                id=msg_data.get("id"),
                sessionId=msg_data.get("session_id"),
                content=msg_data.get("content"),
                isFromUser=msg_data.get("is_from_user"),
                createdAt=msg_data.get("created_at")
            )

        except Exception as e:
            logger.error(f"Failed to save message: {str(e)}", exc_info=True)
            raise


# Global repository instance
chat_repository = ChatRepository()
