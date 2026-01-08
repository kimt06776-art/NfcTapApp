"""
FastAPI router for pathway suggestion endpoint.
"""

import logging
from fastapi import APIRouter, HTTPException
from app.models import PathwayRequest, PathwayApiResponse
from app.chains.pipeline import generate_pathway

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post("/pathway", response_model=PathwayApiResponse)
async def create_pathway(request: PathwayRequest):
    """
    Generate pathway suggestion based on user input.

    Args:
        request: PathwayRequest with user_input

    Returns:
        PathwayApiResponse: Generated pathway response

    Raises:
        HTTPException: If generation fails
    """
    try:
        # Generate pathway using LangChain pipeline
        pathway_response = await generate_pathway(request.user_input)

        # TODO: Save to Supabase (chat_messages table)
        # message_id = await save_to_supabase(request, pathway_response)

        return PathwayApiResponse(
            success=True,
            data=pathway_response,
            error=None,
            message_id=None  # TODO: Return actual message_id
        )

    except Exception as e:
        logger.error(f"Pathway generation failed: {str(e)}", exc_info=True)

        return PathwayApiResponse(
            success=False,
            data=None,
            error=str(e),
            message_id=None
        )
