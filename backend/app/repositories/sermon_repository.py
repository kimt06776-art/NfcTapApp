"""
Sermon repository for database operations.

Migrated from frontend SermonData.kt
"""

from supabase import Client
from app.models.sermon import SermonDto


class SermonRepository:
    """Repository for sermon database operations."""

    def __init__(self, supabase_client: Client):
        self.supabase = supabase_client

    async def get_all_sermons(self) -> list[SermonDto]:
        """
        Get all active sermons ordered by published_at descending.

        Returns:
            List of SermonDto

        Raises:
            Exception: If database query fails
        """
        response = self.supabase.table("sermons") \
            .select("*") \
            .eq("is_active", True) \
            .order("published_at", desc=True) \
            .execute()

        return [SermonDto(**sermon) for sermon in response.data]

    async def get_latest_sermon(self) -> SermonDto | None:
        """
        Get the latest sermon ordered by published_at.

        Returns:
            SermonDto if found, None otherwise

        Raises:
            Exception: If database query fails
        """
        response = self.supabase.table("sermons") \
            .select("*") \
            .eq("is_active", True) \
            .order("published_at", desc=True) \
            .limit(1) \
            .execute()

        if response.data:
            return SermonDto(**response.data[0])
        return None

    async def get_sermon_by_id(self, sermon_id: str) -> SermonDto | None:
        """
        Get sermon by ID.

        Args:
            sermon_id: Sermon UUID

        Returns:
            SermonDto if found, None otherwise

        Raises:
            Exception: If database query fails
        """
        response = self.supabase.table("sermons") \
            .select("*") \
            .eq("id", sermon_id) \
            .execute()

        if response.data:
            return SermonDto(**response.data[0])
        return None
