"""
FastAPI router for sermon note operations.

Includes:
- Create sermon note
- Get all notes for user
- Get single note
- Update note
- Delete note
"""

import logging
from fastapi import APIRouter, HTTPException, Query
from app.models.sermon_note import (
    SermonNoteCreateRequest,
    SermonNoteUpdateRequest,
    SermonNoteListResponse,
    SermonNoteResponse,
    SermonNoteDeleteResponse
)
from app.repositories.sermon_note_repository import sermon_note_repository

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post("/sermon-notes", response_model=SermonNoteResponse)
async def create_sermon_note(request: SermonNoteCreateRequest):
    """
    Create a new sermon note.

    Args:
        request: SermonNoteCreateRequest with userId, title, content, scripture

    Returns:
        SermonNoteResponse: Created sermon note
    """
    try:
        note = await sermon_note_repository.create(
            user_id=request.user_id,
            title=request.title,
            content=request.content,
            scripture=request.scripture
        )

        return SermonNoteResponse(
            success=True,
            note=note,
            error=None
        )

    except Exception as e:
        logger.error(f"Create sermon note failed: {str(e)}", exc_info=True)
        return SermonNoteResponse(
            success=False,
            note=None,
            error=str(e)
        )


@router.get("/sermon-notes", response_model=SermonNoteListResponse)
async def get_sermon_notes(user_id: str = Query(..., alias="userId")):
    """
    Get all sermon notes for a user.

    Args:
        user_id: User ID (query parameter)

    Returns:
        SermonNoteListResponse: List of sermon notes
    """
    try:
        notes = await sermon_note_repository.get_all(user_id)

        return SermonNoteListResponse(
            success=True,
            notes=notes,
            error=None
        )

    except Exception as e:
        logger.error(f"Get sermon notes failed: {str(e)}", exc_info=True)
        return SermonNoteListResponse(
            success=False,
            notes=None,
            error=str(e)
        )


@router.get("/sermon-notes/{note_id}", response_model=SermonNoteResponse)
async def get_sermon_note(note_id: str):
    """
    Get a single sermon note by ID.

    Args:
        note_id: Note ID (path parameter)

    Returns:
        SermonNoteResponse: Sermon note
    """
    try:
        note = await sermon_note_repository.get_by_id(note_id)

        if note is None:
            return SermonNoteResponse(
                success=False,
                note=None,
                error="Sermon note not found"
            )

        return SermonNoteResponse(
            success=True,
            note=note,
            error=None
        )

    except Exception as e:
        logger.error(f"Get sermon note failed: {str(e)}", exc_info=True)
        return SermonNoteResponse(
            success=False,
            note=None,
            error=str(e)
        )


@router.patch("/sermon-notes/{note_id}", response_model=SermonNoteResponse)
async def update_sermon_note(note_id: str, request: SermonNoteUpdateRequest):
    """
    Update a sermon note.

    Args:
        note_id: Note ID (path parameter)
        request: SermonNoteUpdateRequest with optional title, content, scripture

    Returns:
        SermonNoteResponse: Updated sermon note
    """
    try:
        note = await sermon_note_repository.update(
            note_id=note_id,
            title=request.title,
            content=request.content,
            scripture=request.scripture
        )

        return SermonNoteResponse(
            success=True,
            note=note,
            error=None
        )

    except Exception as e:
        logger.error(f"Update sermon note failed: {str(e)}", exc_info=True)
        return SermonNoteResponse(
            success=False,
            note=None,
            error=str(e)
        )


@router.delete("/sermon-notes/{note_id}", response_model=SermonNoteDeleteResponse)
async def delete_sermon_note(note_id: str):
    """
    Delete a sermon note.

    Args:
        note_id: Note ID (path parameter)

    Returns:
        SermonNoteDeleteResponse: Success response
    """
    try:
        await sermon_note_repository.delete(note_id)

        return SermonNoteDeleteResponse(
            success=True,
            message=f"Sermon note {note_id} deleted",
            error=None
        )

    except Exception as e:
        logger.error(f"Delete sermon note failed: {str(e)}", exc_info=True)
        return SermonNoteDeleteResponse(
            success=False,
            message=None,
            error=str(e)
        )
