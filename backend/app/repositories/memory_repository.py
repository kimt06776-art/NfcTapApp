"""
Memory Repository - Long-term memory storage in Supabase.

Handles:
- User profiles (preferences, interests)
- Conversation summaries
- Key facts about users
"""

import logging
from datetime import datetime
from typing import Optional
from app.config import settings

logger = logging.getLogger(__name__)


class MemoryRepository:
    """Repository for long-term memory storage."""

    def __init__(self):
        """Initialize Supabase client."""
        from supabase import create_client
        self.client = create_client(settings.SUPABASE_URL, settings.SUPABASE_KEY)

    # ==================== User Profile ====================

    async def get_user_profile(self, user_id: str) -> Optional[dict]:
        """
        Get user profile with preferences and interests.

        Returns:
            User profile dict or None if not found
        """
        try:
            response = self.client.table("user_profiles").select("*").eq(
                "user_id", user_id
            ).execute()

            if response.data:
                return response.data[0]
            return None

        except Exception as e:
            logger.error(f"Failed to get user profile: {e}")
            return None

    async def upsert_user_profile(
        self,
        user_id: str,
        name: Optional[str] = None,
        preferred_bible_book: Optional[str] = None,
        interests: Optional[list[str]] = None,
        prayer_topics: Optional[list[str]] = None,
    ) -> bool:
        """
        Create or update user profile.

        Args:
            user_id: User ID
            name: User's name (optional)
            preferred_bible_book: Favorite Bible book
            interests: List of interests
            prayer_topics: Prayer request topics

        Returns:
            True if successful
        """
        try:
            data = {
                "user_id": user_id,
                "updated_at": datetime.utcnow().isoformat(),
            }

            if name is not None:
                data["name"] = name
            if preferred_bible_book is not None:
                data["preferred_bible_book"] = preferred_bible_book
            if interests is not None:
                data["interests"] = interests
            if prayer_topics is not None:
                data["prayer_topics"] = prayer_topics

            self.client.table("user_profiles").upsert(data).execute()
            return True

        except Exception as e:
            logger.error(f"Failed to upsert user profile: {e}")
            return False

    # ==================== Key Facts ====================

    async def get_key_facts(self, user_id: str, limit: int = 20) -> list[dict]:
        """
        Get key facts about a user.

        These are important pieces of information learned during conversations.
        Example: "사용자는 요한복음을 좋아한다", "직장에서 스트레스를 받고 있다"

        Args:
            user_id: User ID
            limit: Maximum number of facts to return

        Returns:
            List of key facts
        """
        try:
            response = self.client.table("user_key_facts").select("*").eq(
                "user_id", user_id
            ).order("created_at", desc=True).limit(limit).execute()

            return response.data or []

        except Exception as e:
            logger.error(f"Failed to get key facts: {e}")
            return []

    async def add_key_fact(
        self,
        user_id: str,
        fact: str,
        category: str = "general",
        confidence: float = 0.8
    ) -> bool:
        """
        Add a key fact about a user.

        Args:
            user_id: User ID
            fact: The fact to store
            category: Category (general, preference, concern, prayer, relationship)
            confidence: Confidence score (0.0-1.0)

        Returns:
            True if successful
        """
        try:
            self.client.table("user_key_facts").insert({
                "user_id": user_id,
                "fact": fact,
                "category": category,
                "confidence": confidence,
                "created_at": datetime.utcnow().isoformat(),
            }).execute()
            return True

        except Exception as e:
            logger.error(f"Failed to add key fact: {e}")
            return False

    # ==================== Conversation Summaries ====================

    async def get_conversation_summaries(
        self,
        user_id: str,
        limit: int = 5
    ) -> list[dict]:
        """
        Get recent conversation summaries for a user.

        Args:
            user_id: User ID
            limit: Maximum number of summaries

        Returns:
            List of conversation summaries
        """
        try:
            response = self.client.table("conversation_summaries").select("*").eq(
                "user_id", user_id
            ).order("created_at", desc=True).limit(limit).execute()

            return response.data or []

        except Exception as e:
            logger.error(f"Failed to get conversation summaries: {e}")
            return []

    async def save_conversation_summary(
        self,
        user_id: str,
        session_id: str,
        summary: str,
        topics: list[str],
        emotional_tone: Optional[str] = None
    ) -> bool:
        """
        Save a conversation summary.

        Args:
            user_id: User ID
            session_id: Chat session ID
            summary: Summary of the conversation
            topics: Topics discussed
            emotional_tone: Overall emotional tone (e.g., "hopeful", "anxious")

        Returns:
            True if successful
        """
        try:
            self.client.table("conversation_summaries").insert({
                "user_id": user_id,
                "session_id": session_id,
                "summary": summary,
                "topics": topics,
                "emotional_tone": emotional_tone,
                "created_at": datetime.utcnow().isoformat(),
            }).execute()
            return True

        except Exception as e:
            logger.error(f"Failed to save conversation summary: {e}")
            return False

    # ==================== Memory Context Builder ====================

    async def build_memory_context(self, user_id: str) -> str:
        """
        Build a memory context string for the agent.

        Combines user profile, key facts, and recent summaries
        into a context that can be added to the system prompt.

        Args:
            user_id: User ID

        Returns:
            Memory context string
        """
        context_parts = []

        # User profile
        profile = await self.get_user_profile(user_id)
        if profile:
            profile_parts = []
            if profile.get("name"):
                profile_parts.append(f"이름: {profile['name']}")
            if profile.get("preferred_bible_book"):
                profile_parts.append(f"좋아하는 성경책: {profile['preferred_bible_book']}")
            if profile.get("interests"):
                profile_parts.append(f"관심사: {', '.join(profile['interests'])}")
            if profile.get("prayer_topics"):
                profile_parts.append(f"기도 주제: {', '.join(profile['prayer_topics'])}")

            if profile_parts:
                context_parts.append("## 사용자 정보\n" + "\n".join(profile_parts))

        # Key facts
        facts = await self.get_key_facts(user_id, limit=10)
        if facts:
            fact_strings = [f"- {f['fact']}" for f in facts]
            context_parts.append("## 알고 있는 사실\n" + "\n".join(fact_strings))

        # Recent conversation summaries
        summaries = await self.get_conversation_summaries(user_id, limit=3)
        if summaries:
            summary_strings = []
            for s in summaries:
                summary_strings.append(f"- {s['summary']}")
            context_parts.append("## 최근 대화 요약\n" + "\n".join(summary_strings))

        if context_parts:
            return "\n\n".join(context_parts)
        return ""


# Global repository instance
memory_repository = MemoryRepository()
