"""
Output parsers for LangChain pipeline.
Parses LLM output into PathwayResponse Pydantic model.
"""

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.exceptions import OutputParserException
from app.models import PathwayResponse
import json
import logging

logger = logging.getLogger(__name__)


class PathwayOutputParser(PydanticOutputParser):
    """
    Custom output parser for PathwayResponse.

    Extends PydanticOutputParser with additional validation and error handling.
    """

    def __init__(self):
        super().__init__(pydantic_object=PathwayResponse)

    def parse(self, text: str) -> PathwayResponse:
        """
        Parse LLM output text into PathwayResponse.

        Args:
            text: Raw LLM output (should be JSON)

        Returns:
            PathwayResponse: Parsed and validated response

        Raises:
            OutputParserException: If parsing fails
        """
        try:
            # Try to parse as JSON
            data = json.loads(text)

            # Validate with Pydantic
            return PathwayResponse(**data)

        except json.JSONDecodeError as e:
            logger.error(f"JSON decode error: {e}")
            logger.error(f"Raw output: {text[:500]}")  # Log first 500 chars
            raise OutputParserException(
                f"Failed to parse JSON: {str(e)}\nRaw output: {text[:200]}"
            )
        except Exception as e:
            logger.error(f"Validation error: {e}")
            logger.error(f"Parsed data: {data if 'data' in locals() else 'N/A'}")
            raise OutputParserException(
                f"Failed to validate PathwayResponse: {str(e)}"
            )

    def get_format_instructions(self) -> str:
        """
        Get format instructions for the LLM.

        Returns:
            str: JSON schema description
        """
        return f"""You must respond with valid JSON matching this schema:
{self.pydantic_object.model_json_schema()}

Example:
{{
  "pathway": "word",
  "verse": {{
    "book": "시편",
    "chapter": 23,
    "verse": "1",
    "text": "여호와는 나의 목자시니 내게 부족함이 없으리로다"
  }},
  "brief_connection": "오늘 하루도 주님께서 함께하십니다.",
  "crisis_detected": false
}}"""


def create_pathway_parser() -> PathwayOutputParser:
    """
    Factory function to create PathwayOutputParser instance.

    Returns:
        PathwayOutputParser: Configured parser
    """
    return PathwayOutputParser()
