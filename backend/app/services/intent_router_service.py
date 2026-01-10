"""
Intent Router Service - Classifies user messages and routes to appropriate agents.

Uses GPT-4o-mini for fast, cost-effective intent classification with structured output.
"""

import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from app.config import settings
from app.models.intent import IntentType, IntentClassification

logger = logging.getLogger(__name__)


class IntentRouterService:
    """Service for classifying user intent and routing to appropriate agents."""

    CLASSIFICATION_PROMPT = """당신은 사용자 메시지의 의도를 분류하는 전문가입니다.

사용자 메시지를 다음 5가지 카테고리 중 하나로 분류하세요:

1. **bible_reading** - 성경 구절 찾기/읽기 요청 (네비게이션)
   - "요한복음 14장 찾아줘"
   - "창세기 1장 1절 보여줘"
   - "마태복음 5장 읽고 싶어"
   - "로마서 8장 28절"
   - 특정 책, 장, 절을 찾아달라는 요청

2. **bible_study** - 성경 공부, 해석, 의미 질문
   - "요한복음 3:16이 무슨 뜻이야?"
   - "바울이 말한 사랑의 의미가 뭐야?"
   - "이 구절의 역사적 배경이 궁금해"
   - "성경에서 ~ 에 대해 알려줘"

3. **counseling** - 위로, 상담, 고민 상담
   - "요즘 힘들어..."
   - "불안하고 걱정돼"
   - "어떻게 해야 할지 모르겠어"
   - 감정 표현, 고민 토로

4. **prayer** - 기도 요청, 기도문 작성
   - "기도해줘"
   - "함께 기도하자"
   - "기도문 작성해줘"
   - "~를 위해 기도해줘"

5. **general** - 일반 대화, 인사, 기타
   - "안녕"
   - "뭐해?"
   - 위 카테고리에 해당하지 않는 모든 것

분류 시 주의사항:
- 성경 구절을 "찾아줘", "보여줘", "읽고 싶어" 등의 요청은 bible_reading
- 성경 구절의 "의미", "뜻", "해석" 질문은 bible_study
- 성경 구절이 언급되더라도 위로/상담 목적이면 counseling으로 분류
- 확실하지 않으면 general로 분류
- confidence는 확신 정도를 0.0~1.0으로 표현"""

    def __init__(self):
        """Initialize the router with a fast, small model."""
        # Use gpt-4o-mini for fast classification
        self.llm = ChatOpenAI(
            model="gpt-4o-mini",
            openai_api_key=settings.OPENAI_API_KEY,
            temperature=0.0,  # Deterministic classification
            max_tokens=200
        )
        # Use structured output for guaranteed schema
        self.classifier = self.llm.with_structured_output(IntentClassification)

    async def classify_intent(self, user_message: str) -> IntentClassification:
        """
        Classify user message intent.

        Args:
            user_message: The user's message to classify

        Returns:
            IntentClassification with intent type, confidence, and reasoning
        """
        try:
            messages = [
                SystemMessage(content=self.CLASSIFICATION_PROMPT),
                HumanMessage(content=f"사용자 메시지: {user_message}")
            ]

            result = await self.classifier.ainvoke(messages)
            logger.info(f"Intent classified: {result.intent} (confidence: {result.confidence})")
            return result

        except Exception as e:
            logger.error(f"Intent classification failed: {e}")
            # Fallback to general on error
            return IntentClassification(
                intent=IntentType.GENERAL,
                confidence=0.5,
                reasoning="분류 실패로 인한 기본값"
            )

    def should_use_structured_response(self, intent: IntentType) -> bool:
        """
        Determine if the intent should use structured output.

        Args:
            intent: Classified intent type

        Returns:
            True if structured output should be used
        """
        return intent == IntentType.BIBLE_STUDY


# Global service instance
intent_router_service = IntentRouterService()
