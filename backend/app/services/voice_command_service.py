"""
Voice command analysis service using OpenAI.

Uses OpenAI to analyze natural language voice commands and extract actionable intents.
"""

import logging
from openai import AsyncOpenAI
from app.config import settings
from app.models.voice_command import VoiceCommandAction, VoiceCommandAnalysis

logger = logging.getLogger(__name__)


class VoiceCommandService:
    """Service for analyzing voice commands using OpenAI."""

    SYSTEM_PROMPT = """
당신은 음성 명령을 분석하는 AI 어시스턴트입니다.
사용자가 말한 내용을 분석하여 실행할 액션을 결정합니다.

사용 가능한 액션:
- navigate_home: 홈 화면으로 이동 (예: "홈으로 가줘", "처음으로")
- navigate_sermon: 설교 목록 화면으로 이동 (예: "설교 보여줘", "목사님 설교")
- navigate_chat: 채팅 화면으로 이동 (예: "채팅", "상담하고 싶어")
- navigate_pathway: 영적 안내 화면으로 이동 (예: "영적 안내", "말씀 추천")
- show_daily_verse: 오늘의 말씀 표시 (예: "오늘 말씀", "오늘의 성경 구절")
- start_chat: 새 채팅 시작 (예: "새 대화", "새로 시작")
- play_latest_sermon: 최신 설교 재생 (예: "최신 설교", "이번 주 설교")
- unknown: 의도를 파악할 수 없는 경우

분석 지침:
1. 사용자의 의도를 정확하게 파악하세요
2. 가장 적합한 액션을 선택하세요
3. 신뢰도(confidence)는 0.0~1.0 사이로 평가하세요
4. 필요한 경우 parameters에 추가 정보를 담으세요
5. 사용자에게 친근한 확인 메시지를 작성하세요
""".strip()

    def __init__(self):
        """Initialize voice command service with OpenAI client."""
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = settings.OPENAI_MODEL

    async def analyze_command(self, text: str) -> VoiceCommandAnalysis:
        """
        Analyze voice command text using OpenAI.

        Args:
            text: Voice-to-text converted user command

        Returns:
            VoiceCommandAnalysis: Analyzed command with action and parameters
        """
        try:
            logger.info(f"Analyzing voice command: {text}")

            # Call OpenAI with structured output
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": f"사용자 명령: {text}"}
                ],
                response_format={
                    "type": "json_schema",
                    "json_schema": {
                        "name": "voice_command_analysis",
                        "strict": True,
                        "schema": {
                            "type": "object",
                            "properties": {
                                "action": {
                                    "type": "string",
                                    "enum": [action.value for action in VoiceCommandAction]
                                },
                                "confidence": {
                                    "type": "number",
                                    "minimum": 0.0,
                                    "maximum": 1.0
                                },
                                "parameters": {
                                    "type": "object",
                                    "additionalProperties": True
                                },
                                "message": {
                                    "type": "string"
                                }
                            },
                            "required": ["action", "confidence", "message"],
                            "additionalProperties": False
                        }
                    }
                },
                temperature=0.3,
                max_tokens=200
            )

            # Parse response
            content = response.choices[0].message.content
            if not content:
                raise ValueError("Empty response from OpenAI")

            import json
            result = json.loads(content)

            analysis = VoiceCommandAnalysis(
                action=VoiceCommandAction(result["action"]),
                confidence=result["confidence"],
                parameters=result.get("parameters"),
                message=result["message"]
            )

            logger.info(f"Analysis complete: action={analysis.action}, confidence={analysis.confidence}")
            return analysis

        except Exception as e:
            logger.error(f"Voice command analysis error: {str(e)}", exc_info=True)
            # Return unknown action as fallback
            return VoiceCommandAnalysis(
                action=VoiceCommandAction.UNKNOWN,
                confidence=0.0,
                parameters=None,
                message="죄송해요, 명령을 이해하지 못했어요. 다시 말씀해주세요."
            )


# Global service instance
voice_command_service = VoiceCommandService()
