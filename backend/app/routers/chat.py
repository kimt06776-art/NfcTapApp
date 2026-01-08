"""
FastAPI router for chat operations.

Includes:
- Chat streaming with OpenAI
- Session CRUD operations
- Message CRUD operations
"""

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
    ChatMessageInsert
)
from app.services.openai_service import openai_service
from app.repositories.chat_repository import chat_repository

logger = logging.getLogger(__name__)

router = APIRouter()


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
