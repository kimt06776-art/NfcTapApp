"""
OpenAI service for chat streaming.

Migrated from frontend OpenAIClient.kt to backend for security.
"""

import logging
from typing import AsyncGenerator
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
from app.config import settings

logger = logging.getLogger(__name__)


class OpenAIService:
    """Service for OpenAI chat streaming operations."""

    SYSTEM_PROMPT = """
당신은 따뜻하고 친근한 신앙 상담사입니다.
사용자의 이야기를 경청하고, 공감하며, 적절한 성경 말씀과 함께 위로와 격려를 전합니다.

대화 지침:
- 따뜻하고 친근한 말투를 사용하세요
- 사용자의 감정에 공감하세요
- 필요할 때 적절한 성경 구절을 인용하세요
- 판단하지 말고 경청하세요
- 답변은 간결하게 (3-4문장 정도)
""".strip()

    def __init__(self):
        """Initialize OpenAI service with LangChain client."""
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL,
            openai_api_key=settings.OPENAI_API_KEY,
            temperature=settings.LANGCHAIN_TEMPERATURE,
            max_tokens=settings.LANGCHAIN_MAX_TOKENS,
            streaming=True
        )

    async def chat_stream(
        self,
        user_message: str,
        conversation_history: list[dict] | None = None
    ) -> AsyncGenerator[str, None]:
        """
        Stream chat response using OpenAI.

        Args:
            user_message: User's message
            conversation_history: List of previous messages [{"role": "user|assistant", "content": "..."}]

        Yields:
            str: Streaming token chunks
        """
        try:
            # Build message list
            messages = [SystemMessage(content=self.SYSTEM_PROMPT)]

            # Add conversation history
            if conversation_history:
                for msg in conversation_history:
                    if msg.get("role") == "user":
                        messages.append(HumanMessage(content=msg["content"]))
                    elif msg.get("role") == "assistant":
                        messages.append(AIMessage(content=msg["content"]))

            # Add current user message
            messages.append(HumanMessage(content=user_message))

            # Stream response
            async for chunk in self.llm.astream(messages):
                if chunk.content:
                    yield chunk.content

        except Exception as e:
            logger.error(f"OpenAI streaming error: {str(e)}", exc_info=True)
            raise


# Global service instance
openai_service = OpenAIService()
