"""
Sermon note repository for Supabase operations.
"""

import logging
from typing import Optional
from app.models.sermon_note import SermonNoteDto
from app.repositories.supabase_client import get_supabase_client

logger = logging.getLogger(__name__)


class SermonNoteRepository:
    """Repository for sermon notes CRUD operations."""

    def __init__(self):
        """Initialize repository with Supabase client."""
        self.supabase = get_supabase_client()
        self.table_name = "sermon_notes"

    async def create(
        self,
        user_id: str,
        title: str,
        content: str,
        scripture: Optional[str] = None
    ) -> SermonNoteDto:
        """
        Create a new sermon note.

        Args:
            user_id: User ID
            title: Sermon title
            content: Note content
            scripture: Scripture reference (optional)

        Returns:
            SermonNoteDto: Created sermon note

        Raises:
            Exception: If creation fails
        """
        try:
            data = {
                "user_id": user_id,
                "title": title,
                "content": content,
                "scripture": scripture
            }

            response = self.supabase.table(self.table_name).insert(data).execute()

            if not response.data:
                raise Exception("Failed to create sermon note")

            note_data = response.data[0]

            return SermonNoteDto(
                id=note_data.get("id"),
                userId=note_data.get("user_id"),
                title=note_data.get("title"),
                content=note_data.get("content"),
                scripture=note_data.get("scripture"),
                createdAt=note_data.get("created_at"),
                updatedAt=note_data.get("updated_at")
            )

        except Exception as e:
            logger.error(f"Failed to create sermon note: {str(e)}", exc_info=True)
            raise

    async def get_all(self, user_id: str) -> list[SermonNoteDto]:
        """
        Get all sermon notes for a user.

        Args:
            user_id: User ID

        Returns:
            list[SermonNoteDto]: List of sermon notes ordered by created_at desc
        """
        try:
            response = self.supabase.table(self.table_name) \
                .select("*") \
                .eq("user_id", user_id) \
                .order("created_at", desc=True) \
                .execute()

            notes = []
            for note_data in response.data:
                notes.append(SermonNoteDto(
                    id=note_data.get("id"),
                    userId=note_data.get("user_id"),
                    title=note_data.get("title"),
                    content=note_data.get("content"),
                    scripture=note_data.get("scripture"),
                    createdAt=note_data.get("created_at"),
                    updatedAt=note_data.get("updated_at")
                ))

            return notes

        except Exception as e:
            logger.error(f"Failed to get sermon notes: {str(e)}", exc_info=True)
            raise

    async def get_by_id(self, note_id: str) -> Optional[SermonNoteDto]:
        """
        Get a sermon note by ID.

        Args:
            note_id: Note ID

        Returns:
            SermonNoteDto or None if not found
        """
        try:
            response = self.supabase.table(self.table_name) \
                .select("*") \
                .eq("id", note_id) \
                .single() \
                .execute()

            if not response.data:
                return None

            note_data = response.data

            return SermonNoteDto(
                id=note_data.get("id"),
                userId=note_data.get("user_id"),
                title=note_data.get("title"),
                content=note_data.get("content"),
                scripture=note_data.get("scripture"),
                createdAt=note_data.get("created_at"),
                updatedAt=note_data.get("updated_at")
            )

        except Exception as e:
            logger.error(f"Failed to get sermon note: {str(e)}", exc_info=True)
            raise

    async def update(
        self,
        note_id: str,
        title: Optional[str] = None,
        content: Optional[str] = None,
        scripture: Optional[str] = None
    ) -> SermonNoteDto:
        """
        Update a sermon note.

        Args:
            note_id: Note ID
            title: New title (optional)
            content: New content (optional)
            scripture: New scripture (optional)

        Returns:
            SermonNoteDto: Updated sermon note

        Raises:
            Exception: If update fails
        """
        try:
            update_data = {"updated_at": "now()"}

            if title is not None:
                update_data["title"] = title
            if content is not None:
                update_data["content"] = content
            if scripture is not None:
                update_data["scripture"] = scripture

            response = self.supabase.table(self.table_name) \
                .update(update_data) \
                .eq("id", note_id) \
                .execute()

            if not response.data:
                raise Exception("Failed to update sermon note")

            note_data = response.data[0]

            return SermonNoteDto(
                id=note_data.get("id"),
                userId=note_data.get("user_id"),
                title=note_data.get("title"),
                content=note_data.get("content"),
                scripture=note_data.get("scripture"),
                createdAt=note_data.get("created_at"),
                updatedAt=note_data.get("updated_at")
            )

        except Exception as e:
            logger.error(f"Failed to update sermon note: {str(e)}", exc_info=True)
            raise

    async def delete(self, note_id: str) -> None:
        """
        Delete a sermon note.

        Args:
            note_id: Note ID

        Raises:
            Exception: If deletion fails
        """
        try:
            self.supabase.table(self.table_name) \
                .delete() \
                .eq("id", note_id) \
                .execute()

        except Exception as e:
            logger.error(f"Failed to delete sermon note: {str(e)}", exc_info=True)
            raise


# Global repository instance
sermon_note_repository = SermonNoteRepository()
