"""
Sermon API router.

Migrated from frontend SermonData.kt for security.
"""

from fastapi import APIRouter, HTTPException, Depends
from app.repositories.sermon_repository import SermonRepository
from app.repositories.supabase_client import get_supabase_client
from app.models.sermon import SermonListResponse, SermonDetailResponse
from supabase import Client

router = APIRouter()


def get_sermon_repository(supabase: Client = Depends(get_supabase_client)) -> SermonRepository:
    """Dependency: Get sermon repository instance."""
    return SermonRepository(supabase)


@router.get("/sermons", response_model=SermonListResponse)
async def get_all_sermons(repo: SermonRepository = Depends(get_sermon_repository)):
    """
    Get all active sermons.

    Returns:
        SermonListResponse: List of all active sermons ordered by published_at descending
    """
    try:
        sermons = await repo.get_all_sermons()
        return SermonListResponse(success=True, sermons=sermons)
    except Exception as e:
        return SermonListResponse(success=False, error=str(e))


@router.get("/sermons/latest", response_model=SermonDetailResponse)
async def get_latest_sermon(repo: SermonRepository = Depends(get_sermon_repository)):
    """
    Get the latest sermon.

    Returns:
        SermonDetailResponse: Latest sermon or null if not found
    """
    try:
        sermon = await repo.get_latest_sermon()
        if sermon:
            return SermonDetailResponse(success=True, sermon=sermon)
        else:
            return SermonDetailResponse(success=False, error="설교를 찾을 수 없습니다")
    except Exception as e:
        return SermonDetailResponse(success=False, error=str(e))


@router.get("/sermons/{sermon_id}", response_model=SermonDetailResponse)
async def get_sermon_by_id(
    sermon_id: str,
    repo: SermonRepository = Depends(get_sermon_repository)
):
    """
    Get sermon by ID.

    Args:
        sermon_id: Sermon UUID

    Returns:
        SermonDetailResponse: Sermon details or error
    """
    try:
        sermon = await repo.get_sermon_by_id(sermon_id)
        if sermon:
            return SermonDetailResponse(success=True, sermon=sermon)
        else:
            return SermonDetailResponse(success=False, error="설교를 찾을 수 없습니다")
    except Exception as e:
        return SermonDetailResponse(success=False, error=str(e))
