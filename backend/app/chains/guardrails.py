"""
Guardrails for policy enforcement and safety checks.
Based on docs/design/03-policy-guardrails.md
"""

import re
import logging
from typing import Optional
from app.models import PathwayResponse, BibleVerse

logger = logging.getLogger(__name__)


# ==================== Layer 1: Forbidden Phrases ====================

FORBIDDEN_PATTERNS = [
    # 계시 흉내 금지
    r"하나님[이가께서]*.{0,10}(말씀하[시셨습]|알려주[시십]|보여주[시십])",
    r"성령님.*알려주",
    r"하나님의 뜻은.*~하는 것",

    # 감정 진단 금지
    r"(당신의 )?감정은",
    r"느끼시는 것 같",
    r"(우울|불안|화).*겪고.*계신",

    # 상황 추측 금지
    r"힘든 일이.*있으셨",
    r"무슨 일이.*있었",
    r"왜 그렇게.*느끼",

    # 대화 유도 금지
    r"더 말씀해",
    r"자세히.*말씀해",
    r"이야기를.*들려주",

    # 결론 강요 금지
    r"반드시.*하세요",
    r"하지 않으면 안",
    r"성경은.*라고 말합니다"
]


def check_forbidden_phrases(text: str) -> Optional[str]:
    """
    Check if text contains forbidden phrases.

    Args:
        text: Text to check

    Returns:
        Optional[str]: Matched pattern if found, None otherwise
    """
    for pattern in FORBIDDEN_PATTERNS:
        if re.search(pattern, text):
            logger.warning(f"Forbidden pattern detected: {pattern}")
            return pattern
    return None


# ==================== Layer 2: Crisis Detection ====================

CRISIS_KEYWORDS = {
    "critical": [
        "죽고 싶", "자살", "목숨", "죽어버리", "사라지고 싶",
        "세상을 떠나", "살 가치", "삶의 의미가 없"
    ],
    "warning": [
        "너무 힘들", "견딜 수 없", "더 이상 못", "포기하고 싶",
        "아무 의미 없", "혼자", "고독", "외로"
    ]
}


def detect_crisis(user_input: str) -> tuple[bool, str]:
    """
    Detect crisis keywords in user input.

    Args:
        user_input: User's message

    Returns:
        tuple[bool, str]: (is_crisis, level) where level is "critical" or "warning"
    """
    # Check critical keywords
    for keyword in CRISIS_KEYWORDS["critical"]:
        if keyword in user_input:
            logger.warning(f"Critical crisis keyword detected: {keyword}")
            return (True, "critical")

    # Check warning keywords (2+ matches required)
    warning_count = sum(
        1 for keyword in CRISIS_KEYWORDS["warning"] if keyword in user_input
    )
    if warning_count >= 2:
        return (True, "warning")

    return (False, "none")


# ==================== Layer 3: Response Validation ====================

def validate_pathway_response(response: PathwayResponse) -> tuple[bool, Optional[str]]:
    """
    Validate PathwayResponse against policy rules.

    Args:
        response: PathwayResponse to validate

    Returns:
        tuple[bool, Optional[str]]: (is_valid, error_message)
    """
    # Check brief_connection length
    if len(response.brief_connection) > 50:
        return (False, "brief_connection exceeds 50 characters")

    # Check forbidden phrases in all text fields
    text_fields = [response.brief_connection]

    if response.prayer_text:
        text_fields.append(response.prayer_text)
        if len(response.prayer_text) > 200:
            return (False, "prayer_text exceeds 200 characters")

    if response.silence_guide:
        text_fields.append(response.silence_guide)
        if len(response.silence_guide) > 100:
            return (False, "silence_guide exceeds 100 characters")

    if response.verse:
        text_fields.append(response.verse.text)

    for text in text_fields:
        forbidden_pattern = check_forbidden_phrases(text)
        if forbidden_pattern:
            return (False, f"Contains forbidden pattern: {forbidden_pattern}")

    # Check pathway-specific requirements
    if response.pathway == "word" and not response.verse:
        return (False, "pathway 'word' requires verse")

    if response.pathway == "prayer" and not response.prayer_text:
        return (False, "pathway 'prayer' requires prayer_text")

    if response.pathway == "silence" and not response.silence_guide:
        return (False, "pathway 'silence' requires silence_guide")

    return (True, None)


# ==================== Layer 4: Crisis Template ====================

def create_crisis_response() -> PathwayResponse:
    """
    Create a safe crisis response template.

    Returns:
        PathwayResponse: Pre-defined crisis response
    """
    return PathwayResponse(
        pathway="silence",
        silence_guide="지금 이 순간, 당신은 혼자가 아닙니다.",
        brief_connection="전문 상담이 필요할 수 있습니다.",
        crisis_detected=True
    )


# ==================== Guardrail Pipeline ====================

def run_input_guardrails(user_input: str) -> Optional[PathwayResponse]:
    """
    Run input guardrails before LLM processing.

    Args:
        user_input: User's input text

    Returns:
        Optional[PathwayResponse]: Crisis response if detected, None otherwise
    """
    is_crisis, level = detect_crisis(user_input)

    if is_crisis and level == "critical":
        logger.warning("Critical crisis detected in input - returning crisis template")
        return create_crisis_response()

    return None


def run_output_guardrails(response: PathwayResponse) -> tuple[bool, Optional[str]]:
    """
    Run output guardrails after LLM processing.

    Args:
        response: PathwayResponse from LLM

    Returns:
        tuple[bool, Optional[str]]: (is_valid, error_message)
    """
    # If LLM detected crisis, replace with template
    if response.crisis_detected:
        return (False, "crisis_detected")

    # Validate against policy rules
    is_valid, error = validate_pathway_response(response)

    if not is_valid:
        logger.error(f"Response validation failed: {error}")

    return (is_valid, error)


# ==================== Helper Functions ====================

def should_retry(error_message: Optional[str]) -> bool:
    """
    Determine if LLM should retry based on error.

    Args:
        error_message: Error message from validation

    Returns:
        bool: True if should retry, False if should use fallback
    """
    if error_message == "crisis_detected":
        return False  # Don't retry, use crisis template

    # Retry for policy violations
    return True
