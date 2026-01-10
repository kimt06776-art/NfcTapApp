"""
Pydantic models for NFC Pathway backend.

Models are organized by feature:
- user: User models
- chat: Chat session and message models
- auth: Authentication and NFC registration models
- pathway: Pathway suggestion models
- voice_command: Voice command recognition models
"""

# User models
from app.models.user import UserDto

# Chat models
from app.models.chat import (
    ChatSessionDto,
    ChatMessageDto,
    ChatSessionInsert,
    ChatMessageInsert,
    ChatStreamRequest,
    SessionCreateRequest,
    SessionUpdateRequest,
    SessionListResponse,
    MessageListResponse,
    SessionCreateResponse,
    MessageCreateResponse
)

# Auth models
from app.models.auth import (
    NfcRegistrationDto,
    NfcAuthRequest,
    UserRegisterRequest,
    UserValidateRequest,
    AuthResponse
)

# Pathway models
from app.models.pathway import (
    BibleVerse,
    PathwayResponse,
    PathwayRequest,
    PathwayApiResponse
)

# Sermon models
from app.models.sermon import (
    SermonDto,
    SermonListResponse,
    SermonDetailResponse
)

# Voice Command models
from app.models.voice_command import (
    VoiceCommandAction,
    VoiceCommandRequest,
    VoiceCommandAnalysis,
    VoiceCommandResponse
)

# Sermon Note models
from app.models.sermon_note import (
    SermonNoteDto,
    SermonNoteCreateRequest,
    SermonNoteUpdateRequest,
    SermonNoteListResponse,
    SermonNoteResponse,
    SermonNoteDeleteResponse
)

# Intent models
from app.models.intent import (
    IntentType,
    IntentClassification,
    BibleStudyResponse,
    BibleReadingResponse,
    ChatRouterResponse
)

__all__ = [
    # User
    "UserDto",
    # Chat
    "ChatSessionDto",
    "ChatMessageDto",
    "ChatSessionInsert",
    "ChatMessageInsert",
    "ChatStreamRequest",
    "SessionCreateRequest",
    "SessionUpdateRequest",
    "SessionListResponse",
    "MessageListResponse",
    "SessionCreateResponse",
    "MessageCreateResponse",
    # Auth
    "NfcRegistrationDto",
    "NfcAuthRequest",
    "UserRegisterRequest",
    "UserValidateRequest",
    "AuthResponse",
    # Pathway
    "BibleVerse",
    "PathwayResponse",
    "PathwayRequest",
    "PathwayApiResponse",
    # Sermon
    "SermonDto",
    "SermonListResponse",
    "SermonDetailResponse",
    # Voice Command
    "VoiceCommandAction",
    "VoiceCommandRequest",
    "VoiceCommandAnalysis",
    "VoiceCommandResponse",
    # Sermon Note
    "SermonNoteDto",
    "SermonNoteCreateRequest",
    "SermonNoteUpdateRequest",
    "SermonNoteListResponse",
    "SermonNoteResponse",
    "SermonNoteDeleteResponse",
    # Intent
    "IntentType",
    "IntentClassification",
    "BibleStudyResponse",
    "BibleReadingResponse",
    "ChatRouterResponse",
]
