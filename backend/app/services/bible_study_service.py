"""
Bible Study Agent Service - Provides structured Bible study responses.

Uses structured output to guarantee consistent response format for UI rendering.
"""

import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

from app.config import settings
from app.models.intent import BibleStudyResponse

logger = logging.getLogger(__name__)


class BibleStudyService:
    """Service for generating structured Bible study responses."""

    SYSTEM_PROMPT = """당신은 따뜻하고 지혜로운 성경 교사입니다.
사용자의 성경 관련 질문에 깊이 있고 이해하기 쉬운 답변을 제공합니다.

답변 작성 지침:
1. **passage_explanation** (본문 해설)
   - 구절의 핵심 의미를 명확하게 설명
   - 문맥을 고려한 해석 제공
   - 2-3문장으로 간결하게

2. **historical_context** (역사적 배경)
   - 당시 시대적, 문화적 배경 설명
   - 저자와 수신자의 상황
   - 1-2문장으로

3. **key_words** (핵심 단어)
   - 중요한 단어 2-4개 선정
   - 원어(히브리어/그리스어) 의미가 있다면 포함

4. **life_application** (삶의 적용)
   - 오늘날 우리 삶에 어떻게 적용할 수 있는지
   - 구체적이고 실천 가능한 제안
   - 2-3문장으로

5. **reflection_questions** (묵상 질문)
   - 깊이 생각해볼 질문 2-3개
   - 개인적 적용을 돕는 질문

6. **related_verses** (관련 구절)
   - 함께 읽으면 좋은 성경 구절 2-3개
   - "책이름 장:절" 형식으로

모든 답변은 한국어로 작성하세요.
따뜻하고 격려하는 톤을 유지하세요."""

    def __init__(self):
        """Initialize Bible study service with structured output."""
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL,
            openai_api_key=settings.OPENAI_API_KEY,
            temperature=0.7,
            max_tokens=1500
        )
        # Use structured output for guaranteed schema
        self.agent = self.llm.with_structured_output(BibleStudyResponse)

    async def generate_study(
        self,
        user_message: str,
        conversation_history: list[dict] | None = None
    ) -> BibleStudyResponse:
        """
        Generate structured Bible study response.

        Args:
            user_message: User's Bible study question
            conversation_history: Previous conversation for context

        Returns:
            BibleStudyResponse with structured content
        """
        try:
            messages = [SystemMessage(content=self.SYSTEM_PROMPT)]

            # Add conversation history for context
            if conversation_history:
                for msg in conversation_history[-6:]:  # Last 6 messages for context
                    if msg.get("role") == "user":
                        messages.append(HumanMessage(content=msg["content"]))
                    elif msg.get("role") == "assistant":
                        messages.append(AIMessage(content=msg["content"]))

            messages.append(HumanMessage(content=user_message))

            result = await self.agent.ainvoke(messages)
            logger.info("Bible study response generated successfully")
            return result

        except Exception as e:
            logger.error(f"Bible study generation failed: {e}")
            # Return a fallback response
            return BibleStudyResponse(
                passage_explanation="죄송합니다. 답변을 생성하는 중 오류가 발생했습니다. 다시 질문해 주세요.",
                historical_context="정보를 불러올 수 없습니다.",
                key_words=["오류"],
                life_application="잠시 후 다시 시도해 주세요.",
                reflection_questions=["다시 질문해 주시겠어요?"],
                related_verses=["시편 46:10"]
            )


# Global service instance
bible_study_service = BibleStudyService()
