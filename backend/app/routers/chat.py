"""
FastAPI router for chat operations.

Includes:
- Chat streaming with OpenAI
- Intent-based routing (Bible Study, Counseling, Prayer, General)
- Session CRUD operations
- Message CRUD operations
"""

import json
import logging
from typing import AsyncGenerator
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse
from app.models import (
    ChatStreamRequest,
    SessionCreateRequest,
    SessionUpdateRequest,
    SessionListResponse,
    MessageListResponse,
    SessionCreateResponse,
    MessageCreateResponse,
    ChatSessionDto,
    ChatMessageDto,
    ChatMessageInsert,
    IntentType,
    IntentClassification,
    BibleStudyResponse,
    BibleReadingResponse
)
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from app.config import settings
from app.services.openai_service import openai_service
from app.services.intent_router_service import intent_router_service
from app.services.bible_study_service import bible_study_service
from app.repositories.chat_repository import chat_repository

logger = logging.getLogger(__name__)

router = APIRouter()


# ==================== Intent Classification ====================

@router.post("/chat/classify")
async def classify_intent(request: ChatStreamRequest):
    """
    Classify user message intent.

    Returns the intent type so frontend can decide how to handle the response.

    Args:
        request: ChatStreamRequest with userMessage

    Returns:
        IntentClassification with intent, confidence, and reasoning
    """
    try:
        classification = await intent_router_service.classify_intent(request.user_message)

        return {
            "success": True,
            "classification": {
                "intent": classification.intent.value,
                "confidence": classification.confidence,
                "reasoning": classification.reasoning
            }
        }

    except Exception as e:
        logger.error(f"Intent classification failed: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": str(e),
            "classification": {
                "intent": IntentType.GENERAL.value,
                "confidence": 0.5,
                "reasoning": "분류 실패"
            }
        }


# ==================== Bible Study (Structured Response) ====================

@router.post("/chat/bible-study")
async def bible_study(request: ChatStreamRequest):
    """
    Generate structured Bible study response.

    Used when intent is classified as 'bible_study'.

    Args:
        request: ChatStreamRequest with sessionId and userMessage

    Returns:
        Structured BibleStudyResponse
    """
    try:
        # Get conversation history
        messages = await chat_repository.get_messages(request.session_id)
        conversation_history = [
            {"role": "user" if msg.is_from_user else "assistant", "content": msg.content}
            for msg in messages
        ]

        # Save user message
        await chat_repository.save_message(
            session_id=request.session_id,
            content=request.user_message,
            is_from_user=True
        )

        # Generate Bible study response
        response = await bible_study_service.generate_study(
            user_message=request.user_message,
            conversation_history=conversation_history
        )

        # Save AI response as JSON string for history
        response_json = json.dumps(response.model_dump(), ensure_ascii=False)
        await chat_repository.save_message(
            session_id=request.session_id,
            content=response_json,
            is_from_user=False
        )

        return {
            "success": True,
            "response_type": "structured",
            "intent": IntentType.BIBLE_STUDY.value,
            "data": response.model_dump()
        }

    except Exception as e:
        logger.error(f"Bible study failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ==================== Bible Reading (Navigation) ====================

# LLM for parsing Bible references
_bible_parser_llm = None

def get_bible_parser():
    global _bible_parser_llm
    if _bible_parser_llm is None:
        llm = ChatOpenAI(
            model="gpt-4o-mini",
            openai_api_key=settings.OPENAI_API_KEY,
            temperature=0.0,
            max_tokens=200
        )
        _bible_parser_llm = llm.with_structured_output(BibleReadingResponse)
    return _bible_parser_llm

BIBLE_PARSER_PROMPT = """사용자 메시지에서 성경 책 이름, 장, 절 정보를 추출하세요.

규칙:
- book: 성경책 이름 (예: "요한복음", "창세기", "마태복음")
- chapter: 장 번호 (숫자). 장 번호가 명시되지 않으면 1
- verse: 절 번호 (없으면 null)
- verse_end: 끝 절 번호 (범위 지정 시, 예: 1-5절이면 verse=1, verse_end=5)
- is_supported: 정경 성경책이면 true, 외경/위경이면 false
- error_message: 지원하지 않는 책이면 이유 설명

정경 성경 66권만 지원합니다:
- 구약: 창세기, 출애굽기, 레위기, 민수기, 신명기, 여호수아, 사사기, 룻기, 사무엘상, 사무엘하, 열왕기상, 열왕기하, 역대상, 역대하, 에스라, 느헤미야, 에스더, 욥기, 시편, 잠언, 전도서, 아가, 이사야, 예레미야, 예레미야애가, 에스겔, 다니엘, 호세아, 요엘, 아모스, 오바댜, 요나, 미가, 나훔, 하박국, 스바냐, 학개, 스가랴, 말라기
- 신약: 마태복음, 마가복음, 누가복음, 요한복음, 사도행전, 로마서, 고린도전서, 고린도후서, 갈라디아서, 에베소서, 빌립보서, 골로새서, 데살로니가전서, 데살로니가후서, 디모데전서, 디모데후서, 디도서, 빌레몬서, 히브리서, 야고보서, 베드로전서, 베드로후서, 요한일서, 요한이서, 요한삼서, 유다서, 요한계시록

지원하지 않는 책 예시: 에녹서, 토비트, 유딧, 마카비서, 집회서, 지혜서 등 (외경/위경)

예시:
- "요한복음 14장 찾아줘" → book="요한복음", chapter=14, verse=null, is_supported=true
- "에녹서 찾아줘" → book="에녹서", chapter=null, verse=null, is_supported=false, error_message="에녹서는 외경으로 현재 지원하지 않습니다"
- "창세기 찾아줘" → book="창세기", chapter=1, verse=null, is_supported=true"""


@router.post("/chat/bible-reading")
async def bible_reading(request: ChatStreamRequest):
    """
    Parse Bible reference and return navigation info.

    Used when intent is classified as 'bible_reading'.

    Args:
        request: ChatStreamRequest with userMessage containing Bible reference

    Returns:
        BibleReadingResponse with book, chapter, verse info
    """
    try:
        parser = get_bible_parser()
        messages = [
            SystemMessage(content=BIBLE_PARSER_PROMPT),
            HumanMessage(content=request.user_message)
        ]

        result = await parser.ainvoke(messages)
        logger.info(f"Bible reading parsed: {result.book} {result.chapter}:{result.verse} (supported: {result.is_supported})")

        # 지원하지 않는 책인 경우
        if not result.is_supported:
            return {
                "success": False,
                "response_type": "navigation",
                "intent": IntentType.BIBLE_READING.value,
                "error": result.error_message or f"{result.book}은(는) 현재 지원하지 않는 성경책입니다",
                "data": result.model_dump()
            }

        # chapter가 없는 경우 (파싱 실패)
        if result.chapter is None:
            return {
                "success": False,
                "response_type": "navigation",
                "intent": IntentType.BIBLE_READING.value,
                "error": "성경 구절을 파싱할 수 없습니다. 책 이름과 장 번호를 명확히 말씀해주세요.",
                "data": result.model_dump()
            }

        return {
            "success": True,
            "response_type": "navigation",
            "intent": IntentType.BIBLE_READING.value,
            "data": result.model_dump()
        }

    except Exception as e:
        logger.error(f"Bible reading parse failed: {str(e)}", exc_info=True)
        # 예외 발생 시에도 crash 대신 에러 응답 반환
        return {
            "success": False,
            "response_type": "navigation",
            "intent": IntentType.BIBLE_READING.value,
            "error": "성경 구절 파싱 중 오류가 발생했습니다. 다시 시도해주세요.",
            "data": None
        }


# ==================== Smart Chat (Auto-routing) ====================

@router.post("/chat/smart")
async def smart_chat(request: ChatStreamRequest):
    """
    Smart chat endpoint that auto-routes based on intent.

    - bible_study: Returns structured JSON response
    - others: Returns streaming SSE response

    Note: For streaming responses, use /chat/stream after checking intent via /chat/classify
    """
    try:
        # Classify intent
        classification = await intent_router_service.classify_intent(request.user_message)

        if classification.intent == IntentType.BIBLE_STUDY:
            # Return structured response
            messages = await chat_repository.get_messages(request.session_id)
            conversation_history = [
                {"role": "user" if msg.is_from_user else "assistant", "content": msg.content}
                for msg in messages
            ]

            await chat_repository.save_message(
                session_id=request.session_id,
                content=request.user_message,
                is_from_user=True
            )

            response = await bible_study_service.generate_study(
                user_message=request.user_message,
                conversation_history=conversation_history
            )

            response_json = json.dumps(response.model_dump(), ensure_ascii=False)
            await chat_repository.save_message(
                session_id=request.session_id,
                content=response_json,
                is_from_user=False
            )

            return {
                "success": True,
                "response_type": "structured",
                "intent": classification.intent.value,
                "data": response.model_dump()
            }
        else:
            # For streaming, return intent info and let frontend call /chat/stream
            return {
                "success": True,
                "response_type": "stream",
                "intent": classification.intent.value,
                "message": "Use /chat/stream endpoint for streaming response"
            }

    except Exception as e:
        logger.error(f"Smart chat failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ==================== Chat Streaming ====================

@router.post("/chat/stream")
async def chat_stream(request: ChatStreamRequest):
    """
    Stream chat response from OpenAI.

    Migrated from frontend OpenAIClient.kt for security.

    Args:
        request: ChatStreamRequest with sessionId and userMessage

    Returns:
        StreamingResponse: Server-Sent Events stream
    """
    try:
        # Get conversation history from database
        messages = await chat_repository.get_messages(request.session_id)

        # Convert to OpenAI format
        conversation_history = []
        for msg in messages:
            role = "user" if msg.is_from_user else "assistant"
            conversation_history.append({
                "role": role,
                "content": msg.content
            })

        # Save user message first
        await chat_repository.save_message(
            session_id=request.session_id,
            content=request.user_message,
            is_from_user=True
        )

        # Stream generator
        async def generate() -> AsyncGenerator[str, None]:
            full_response = []
            try:
                # Stream from OpenAI
                async for chunk in openai_service.chat_stream(
                    user_message=request.user_message,
                    conversation_history=conversation_history
                ):
                    full_response.append(chunk)
                    # Server-Sent Events format
                    yield f"data: {chunk}\n\n"

                # Save AI response after streaming completes
                complete_response = "".join(full_response)
                await chat_repository.save_message(
                    session_id=request.session_id,
                    content=complete_response,
                    is_from_user=False
                )

                # Send completion signal
                yield "data: [DONE]\n\n"

            except Exception as e:
                logger.error(f"Streaming error: {str(e)}", exc_info=True)
                yield f"data: [ERROR] {str(e)}\n\n"

        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no"  # Disable buffering in nginx
            }
        )

    except Exception as e:
        logger.error(f"Chat stream failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ==================== Session CRUD ====================

@router.post("/chat/sessions", response_model=SessionCreateResponse)
async def create_session(request: SessionCreateRequest):
    """
    Create a new chat session.

    Args:
        request: SessionCreateRequest with userId and optional title

    Returns:
        SessionCreateResponse: Created session
    """
    try:
        session = await chat_repository.create_session(
            user_id=request.user_id,
            title=request.title
        )

        return SessionCreateResponse(
            success=True,
            session=session,
            error=None
        )

    except Exception as e:
        logger.error(f"Session creation failed: {str(e)}", exc_info=True)
        return SessionCreateResponse(
            success=False,
            session=None,
            error=str(e)
        )


@router.get("/chat/sessions", response_model=SessionListResponse)
async def get_sessions(user_id: str = Query(..., alias="userId")):
    """
    Get all sessions for a user.

    Args:
        user_id: User ID (query parameter)

    Returns:
        SessionListResponse: List of sessions
    """
    try:
        sessions = await chat_repository.get_sessions(user_id)

        return SessionListResponse(
            success=True,
            sessions=sessions,
            error=None
        )

    except Exception as e:
        logger.error(f"Get sessions failed: {str(e)}", exc_info=True)
        return SessionListResponse(
            success=False,
            sessions=None,
            error=str(e)
        )


@router.patch("/chat/sessions/{session_id}")
async def update_session_title(session_id: str, request: SessionUpdateRequest):
    """
    Update session title.

    Args:
        session_id: Session ID (path parameter)
        request: SessionUpdateRequest with new title

    Returns:
        Success response
    """
    try:
        await chat_repository.update_session_title(session_id, request.title)

        return {
            "success": True,
            "message": f"Session {session_id} title updated"
        }

    except Exception as e:
        logger.error(f"Update session title failed: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": str(e)
        }


@router.delete("/chat/sessions/{session_id}")
async def delete_session(session_id: str):
    """
    Delete a session (cascade deletes messages).

    Args:
        session_id: Session ID (path parameter)

    Returns:
        Success response
    """
    try:
        await chat_repository.delete_session(session_id)

        return {
            "success": True,
            "message": f"Session {session_id} deleted"
        }

    except Exception as e:
        logger.error(f"Delete session failed: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": str(e)
        }


# ==================== Message CRUD ====================

@router.get("/chat/sessions/{session_id}/messages", response_model=MessageListResponse)
async def get_messages(session_id: str):
    """
    Get all messages for a session.

    Args:
        session_id: Session ID (path parameter)

    Returns:
        MessageListResponse: List of messages
    """
    try:
        messages = await chat_repository.get_messages(session_id)

        return MessageListResponse(
            success=True,
            messages=messages,
            error=None
        )

    except Exception as e:
        logger.error(f"Get messages failed: {str(e)}", exc_info=True)
        return MessageListResponse(
            success=False,
            messages=None,
            error=str(e)
        )


@router.post("/chat/messages", response_model=MessageCreateResponse)
async def save_message(request: ChatMessageInsert):
    """
    Save a new message (manual save, not used during streaming).

    Args:
        request: ChatMessageInsert with sessionId, content, isFromUser

    Returns:
        MessageCreateResponse: Created message
    """
    try:
        message = await chat_repository.save_message(
            session_id=request.session_id,
            content=request.content,
            is_from_user=request.is_from_user
        )

        return MessageCreateResponse(
            success=True,
            message=message,
            error=None
        )

    except Exception as e:
        logger.error(f"Save message failed: {str(e)}", exc_info=True)
        return MessageCreateResponse(
            success=False,
            message=None,
            error=str(e)
        )
