"""
Configuration management using pydantic-settings.
Loads environment variables from .env file.
"""

from pydantic_settings import BaseSettings
from typing import Literal


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # OpenAI Configuration
    OPENAI_API_KEY: str
    OPENAI_MODEL: str = "gpt-4o-2024-08-06"  # Structured output support

    # Supabase Configuration
    SUPABASE_URL: str
    SUPABASE_KEY: str  # Anon key for service-side operations

    # Application Configuration
    ENVIRONMENT: Literal["development", "production"] = "development"
    LOG_LEVEL: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "DEBUG"
    MAX_RETRIES: int = 2

    # LangChain Configuration
    LANGCHAIN_TEMPERATURE: float = 0.7
    LANGCHAIN_MAX_TOKENS: int = 500

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


# Global settings instance
settings = Settings()
