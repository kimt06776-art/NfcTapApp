package com.example.nfctapapp.domain.model

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class NfcScanned(val uid: String) : AuthState()
    data class NfcNotRegistered(val uid: String) : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
