"""
FastAPI main application entry point.
"""

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.config import settings
from app.routers import pathway, chat, auth, sermon
from app.chains.pipeline import health_check_pipeline

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

# Reduce noise from external libraries
logging.getLogger("httpcore").setLevel(logging.WARNING)
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
logging.getLogger("openai").setLevel(logging.WARNING)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager for startup/shutdown events.
    """
    # Startup
    # Health check
    is_healthy = await health_check_pipeline()
    if not is_healthy:
        logger.warning("⚠️ LLM Pipeline health check failed")

    yield

    # Shutdown


# Create FastAPI app
app = FastAPI(
    title="NFC Pathway API",
    description="Backend API for NFC Pathway spiritual companion app",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # TODO: Restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Include routers
app.include_router(pathway.router, prefix="/api", tags=["pathway"])
app.include_router(chat.router, prefix="/api", tags=["chat"])
app.include_router(auth.router, prefix="/api", tags=["auth"])
app.include_router(sermon.router, prefix="/api", tags=["sermon"])


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "NFC Pathway Backend",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health")
async def health():
    """Health check endpoint."""
    pipeline_healthy = await health_check_pipeline()

    return {
        "status": "healthy" if pipeline_healthy else "degraded",
        "pipeline": "ok" if pipeline_healthy else "error",
        "environment": settings.ENVIRONMENT
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.ENVIRONMENT == "development"
    )
