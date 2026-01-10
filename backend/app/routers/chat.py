"""
FastAPI router for chat operations.

Simplified with Agent-based architecture:
- Single agent handles all intents (bible study, counseling, prayer, navigation)
- Agent decides when to use tools vs respond directly
- Session/Message CRUD remains unchanged
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
    ChatMessageInsert,
)
from app.services.agent_service import agent_service
from app.repositories.chat_repository import chat_repository

logger = logging.getLogger(__name__)

router = APIRouter()


# ==================== Agent Chat (Main Endpoint) ====================

@router.post("/chat/agent")
async def agent_chat(request: ChatStreamRequest):
    """
    Main chat endpoint using LangGraph Agent with Memory.

    The agent automatically:
    - Searches Bible verses when needed
    - Navigates to app screens when requested
    - Provides counseling and prayer support
    - Remembers user preferences and key facts (long-term memory)
    - Maintains conversation context (short-term memory via checkpointer)

    Args:
        request: ChatStreamRequest with sessionId and userMessage

    Returns:
        Agent response with optional navigation info
    """
    try:
        # Get user_id from session
        user_id = ""
        conversation_history = []

        if request.session_id:
            # Get session to find user_id
            session = await chat_repository.get_session(request.session_id)
            if session:
                user_id = session.user_id

            # Get conversation history for migration/fallback
            messages = await chat_repository.get_messages(request.session_id)
            conversation_history = [
                {"role": "user" if msg.is_from_user else "assistant", "content": msg.content}
                for msg in messages
            ]

            # Save user message to DB
            await chat_repository.save_message(
                session_id=request.session_id,
                content=request.user_message,
                is_from_user=True
            )

        # Call agent with memory support
        result = await agent_service.chat(
            user_message=request.user_message,
            session_id=request.session_id or "",
            user_id=user_id,
            conversation_history=conversation_history
        )

        # Save AI response to DB
        if request.session_id and result["response"]:
            await chat_repository.save_message(
                session_id=request.session_id,
                content=result["response"],
                is_from_user=False
            )

        return {
            "success": True,
            "response": result["response"],
            "navigation": result["navigation"],
            "tools_used": result["tools_used"],
        }

    except Exception as e:
        logger.error(f"Agent chat failed: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": str(e),
            "response": "죄송합니다. 오류가 발생했어요. 다시 시도해주세요.",
            "navigation": None,
            "tools_used": [],
        }


@router.post("/chat/stream")
async def chat_stream(request: ChatStreamRequest):
    """
    Stream chat response using Agent with Memory.

    Args:
        request: ChatStreamRequest with sessionId and userMessage

    Returns:
        StreamingResponse: Server-Sent Events stream
    """
    try:
        # Get user_id and conversation history
        user_id = ""
        conversation_history = []

        if request.session_id:
            # Get session to find user_id
            session = await chat_repository.get_session(request.session_id)
            if session:
                user_id = session.user_id

            messages = await chat_repository.get_messages(request.session_id)
            conversation_history = [
                {"role": "user" if msg.is_from_user else "assistant", "content": msg.content}
                for msg in messages
            ]

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
                async for chunk in agent_service.chat_stream(
                    user_message=request.user_message,
                    session_id=request.session_id or "",
                    user_id=user_id,
                    conversation_history=conversation_history
                ):
                    full_response.append(chunk)
                    yield f"data: {chunk}\n\n"

                # Save AI response after streaming completes
                if request.session_id:
                    complete_response = "".join(full_response)
                    await chat_repository.save_message(
                        session_id=request.session_id,
                        content=complete_response,
                        is_from_user=False
                    )

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
                "X-Accel-Buffering": "no"
            }
        )

    except Exception as e:
        logger.error(f"Chat stream failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ==================== Session CRUD ====================

@router.post("/chat/sessions", response_model=SessionCreateResponse)
async def create_session(request: SessionCreateRequest):
    """Create a new chat session."""
    try:
        session = await chat_repository.create_session(
            user_id=request.user_id,
            title=request.title
        )
        return SessionCreateResponse(success=True, session=session, error=None)
    except Exception as e:
        logger.error(f"Session creation failed: {str(e)}", exc_info=True)
        return SessionCreateResponse(success=False, session=None, error=str(e))


@router.get("/chat/sessions", response_model=SessionListResponse)
async def get_sessions(user_id: str = Query(..., alias="userId")):
    """Get all sessions for a user."""
    try:
        sessions = await chat_repository.get_sessions(user_id)
        return SessionListResponse(success=True, sessions=sessions, error=None)
    except Exception as e:
        logger.error(f"Get sessions failed: {str(e)}", exc_info=True)
        return SessionListResponse(success=False, sessions=None, error=str(e))


@router.patch("/chat/sessions/{session_id}")
async def update_session_title(session_id: str, request: SessionUpdateRequest):
    """Update session title."""
    try:
        await chat_repository.update_session_title(session_id, request.title)
        return {"success": True, "message": f"Session {session_id} title updated"}
    except Exception as e:
        logger.error(f"Update session title failed: {str(e)}", exc_info=True)
        return {"success": False, "error": str(e)}


@router.delete("/chat/sessions/{session_id}")
async def delete_session(session_id: str):
    """Delete a session (cascade deletes messages)."""
    try:
        await chat_repository.delete_session(session_id)
        return {"success": True, "message": f"Session {session_id} deleted"}
    except Exception as e:
        logger.error(f"Delete session failed: {str(e)}", exc_info=True)
        return {"success": False, "error": str(e)}


# ==================== Message CRUD ====================

@router.get("/chat/sessions/{session_id}/messages", response_model=MessageListResponse)
async def get_messages(session_id: str):
    """Get all messages for a session."""
    try:
        messages = await chat_repository.get_messages(session_id)
        return MessageListResponse(success=True, messages=messages, error=None)
    except Exception as e:
        logger.error(f"Get messages failed: {str(e)}", exc_info=True)
        return MessageListResponse(success=False, messages=None, error=str(e))


@router.post("/chat/messages", response_model=MessageCreateResponse)
async def save_message(request: ChatMessageInsert):
    """Save a new message."""
    try:
        message = await chat_repository.save_message(
            session_id=request.session_id,
            content=request.content,
            is_from_user=request.is_from_user
        )
        return MessageCreateResponse(success=True, message=message, error=None)
    except Exception as e:
        logger.error(f"Save message failed: {str(e)}", exc_info=True)
        return MessageCreateResponse(success=False, message=None, error=str(e))
