"""
Supabase client initialization and helper functions.
Migrated from frontend/app/src/main/java/com/example/nfctapapp/data/remote/SupabaseClient.kt
"""

from supabase import create_client, Client
from app.config import settings

# Global Supabase client instance
supabase: Client = create_client(
    settings.SUPABASE_URL,
    settings.SUPABASE_KEY
)


def get_supabase_client() -> Client:
    """
    Get the global Supabase client instance.

    Returns:
        Client: Configured Supabase client
    """
    return supabase
