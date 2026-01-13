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
- navigate_sermon: 설교 목록 화면으로 이동
  예: "설교 보여줘", "목사님 설교", "설교", "말씀", "예배 영상", "주일 설교", "설교 듣고 싶어", "설교 목록", "목사님 말씀"

- navigate_bible: 성경 화면으로 이동
  예: "성경", "성경 보여줘", "성경 펴줘", "성경 읽을래", "바이블", "말씀 읽기", "성경책"

- navigate_sermon_note: 설교 노트 화면으로 이동
  예: "설교 노트", "노트", "필기", "설교 필기", "노트 열어줘", "메모", "설교 메모", "필기하고 싶어"

- navigate_community: 공동체 화면으로 이동
  예: "공동체", "커뮤니티", "교회", "성도", "모임"

- show_daily_verse: 오늘의 말씀 표시
  예: "오늘 말씀", "오늘의 성경 구절", "오늘의 말씀", "데일리 말씀", "오늘 성경", "매일 말씀", "오늘 구절", "오늘 묵상"

- play_latest_sermon: 최신 설교 재생
  예: "최신 설교", "이번 주 설교", "새 설교", "최근 설교", "지난 주일 설교", "어제 설교"

- unknown: 위 액션에 해당하지 않는 대화형 요청
  예: "요즘 힘들어요", "기도해줘", "위로가 필요해", "고민이 있어"

분석 지침:
1. 사용자의 의도를 정확하게 파악하세요
2. 화면 이동 의도가 명확하면 해당 navigate 액션을 선택하세요
3. 대화/상담/기도 요청은 unknown으로 분류하세요 (채팅으로 전달됨)
4. 신뢰도(confidence)는 0.0~1.0 사이로 평가하세요:
   - 0.9 이상: 명확한 명령 ("설교 보여줘", "성경 펴줘")
   - 0.7-0.9: 의도 추정 가능 ("뭐 볼게 없나", "설교 들을까")
   - 0.5 이하: 불명확한 요청 → unknown 반환
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
                parameters=None,
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
