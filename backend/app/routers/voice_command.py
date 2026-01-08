"""
Voice command API router.

Analyzes natural language voice commands using OpenAI.
"""

from fastapi import APIRouter
from app.models.voice_command import VoiceCommandRequest, VoiceCommandResponse
from app.services.voice_command_service import voice_command_service

router = APIRouter()


@router.post("/voice-command", response_model=VoiceCommandResponse)
async def analyze_voice_command(request: VoiceCommandRequest):
    """
    Analyze voice command text and return actionable intent.

    Args:
        request: VoiceCommandRequest with text and optional userId

    Returns:
        VoiceCommandResponse: Analysis result with action, confidence, and message

    Example:
        POST /api/voice-command
        {
            "text": "오늘 설교 들려줘",
            "userId": "user-123"
        }

        Response:
        {
            "success": true,
            "analysis": {
                "action": "play_latest_sermon",
                "confidence": 0.95,
                "parameters": {"filter": "latest"},
                "message": "최신 설교를 보여드릴게요"
            }
        }
    """
    try:
        analysis = await voice_command_service.analyze_command(request.text)
        return VoiceCommandResponse(success=True, analysis=analysis)
    except Exception as e:
        return VoiceCommandResponse(
            success=False,
            error=f"음성 명령 분석 중 오류가 발생했습니다: {str(e)}"
        )
