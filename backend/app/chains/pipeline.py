"""
Main LangChain pipeline orchestration.
Combines prompts, LLM, parser, and guardrails.
Based on docs/design/04-langchain-pipeline.md
"""

import logging
from typing import Optional
from langchain_openai import ChatOpenAI
from langchain_core.runnables import RunnableSequence

from app.config import settings
from app.models import PathwayResponse
from app.chains.prompts import create_pathway_prompt
from app.chains.parsers import create_pathway_parser
from app.chains.guardrails import (
    run_input_guardrails,
    run_output_guardrails,
    create_crisis_response,
    should_retry
)

logger = logging.getLogger(__name__)


# ==================== Pipeline Configuration ====================

def create_llm() -> ChatOpenAI:
    """
    Create configured ChatOpenAI instance.

    Returns:
        ChatOpenAI: Configured LLM with structured output support
    """
    return ChatOpenAI(
        model=settings.OPENAI_MODEL,
        temperature=settings.LANGCHAIN_TEMPERATURE,
        max_tokens=settings.LANGCHAIN_MAX_TOKENS,
        model_kwargs={
            "response_format": {"type": "json_object"}  # Force JSON output
        }
    )


def create_pathway_chain() -> RunnableSequence:
    """
    Create the main pathway suggestion chain.

    Returns:
        RunnableSequence: LangChain LCEL chain
    """
    prompt = create_pathway_prompt()
    llm = create_llm()
    parser = create_pathway_parser()

    # Create LCEL chain: prompt | llm | parser
    chain = prompt | llm | parser

    return chain


# ==================== Pipeline Execution ====================

async def generate_pathway(
    user_input: str,
    max_retries: Optional[int] = None
) -> PathwayResponse:
    """
    Main pipeline: Generate pathway response with guardrails and retry logic.

    Pipeline steps:
    1. Input crisis detection
    2. Prompt assembly
    3. LLM invocation
    4. Output parsing
    5. Output validation (guardrails)
    6. Retry on failure (max 2 times)
    7. Fallback on persistent failure
    8. Logging

    Args:
        user_input: User's input text
        max_retries: Maximum retry attempts (default from settings)

    Returns:
        PathwayResponse: Generated or fallback response
    """
    if max_retries is None:
        max_retries = settings.MAX_RETRIES

    # Step 1: Input guardrails - crisis detection
    crisis_response = run_input_guardrails(user_input)
    if crisis_response:
        return crisis_response

    # Step 2-4: LLM Pipeline with retry logic
    chain = create_pathway_chain()
    attempt = 0

    while attempt <= max_retries:
        try:
            attempt += 1

            # Invoke chain
            response: PathwayResponse = await chain.ainvoke({"user_input": user_input})

            # Step 5: Output guardrails
            is_valid, error = run_output_guardrails(response)

            if error == "crisis_detected":
                # LLM detected crisis - replace with template
                return create_crisis_response()

            if is_valid:
                return response

            # Invalid response - check if should retry
            if not should_retry(error):
                logger.warning(f"Non-retryable error: {error} - using fallback")
                break

            logger.warning(f"Validation failed (attempt {attempt}): {error}")

            if attempt > max_retries:
                logger.error("Max retries exceeded - using fallback")
                break

        except Exception as e:
            logger.error(f"Pipeline error (attempt {attempt}): {str(e)}")

            if attempt > max_retries:
                logger.error("Max retries exceeded after error - using fallback")
                break

    # Step 7: Fallback
    logger.warning("Using fallback response")
    return create_fallback_response()


# ==================== Fallback Response ====================

def create_fallback_response() -> PathwayResponse:
    """
    Create a safe fallback response when pipeline fails.

    Returns:
        PathwayResponse: Generic safe response
    """
    from app.models import BibleVerse

    return PathwayResponse(
        pathway="word",
        verse=BibleVerse(
            book="시편",
            chapter=46,
            verse="1",
            text="하나님은 우리의 피난처시요 힘이시니 환난 중에 만날 큰 도움이시라"
        ),
        brief_connection="하나님께서 함께하십니다.",
        crisis_detected=False
    )


# ==================== Health Check ====================

async def health_check_pipeline() -> bool:
    """
    Check if LLM pipeline is working.

    Returns:
        bool: True if pipeline is healthy
    """
    try:
        test_response = await generate_pathway("테스트")
        return test_response is not None
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return False
