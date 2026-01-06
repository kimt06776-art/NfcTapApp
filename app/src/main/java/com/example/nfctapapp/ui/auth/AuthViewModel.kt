package com.example.nfctapapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.repository.AuthRepository
import com.example.nfctapapp.data.repository.NfcNotRegisteredException
import com.example.nfctapapp.domain.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _pendingNfcUid = MutableStateFlow<String?>(null)
    val pendingNfcUid: StateFlow<String?> = _pendingNfcUid.asStateFlow()

    private var cacheCheckJob: Job? = null

    init {
        // init에서는 호출하지 않음 - AppNavigation에서 NFC 여부에 따라 호출
    }

    fun checkCachedUser() {
        cacheCheckJob = viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.validateCachedUser().fold(
                onSuccess = { user ->
                    _authState.value = if (user != null) {
                        AuthState.Authenticated(user)
                    } else {
                        AuthState.Unauthenticated
                    }
                },
                onFailure = {
                    _authState.value = AuthState.Unauthenticated
                }
            )
        }
    }

    fun onNfcTagScanned(uid: String) {
        cacheCheckJob?.cancel()
        viewModelScope.launch {
            _authState.value = AuthState.NfcScanned(uid)

            authRepository.authenticateWithNfc(uid).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { exception ->
                    when (exception) {
                        is NfcNotRegisteredException -> {
                            _pendingNfcUid.value = uid
                            _authState.value = AuthState.NfcNotRegistered(uid)
                        }
                        else -> {
                            _authState.value = AuthState.Error(
                                exception.message ?: "인증에 실패했습니다"
                            )
                        }
                    }
                }
            )
        }
    }

    fun registerUser(name: String, phone: String?) {
        val nfcUid = _pendingNfcUid.value ?: return

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.registerUser(name, phone, nfcUid).fold(
                onSuccess = { user ->
                    _pendingNfcUid.value = null
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "등록에 실패했습니다"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun resetToUnauthenticated() {
        _authState.value = AuthState.Unauthenticated
        _pendingNfcUid.value = null
    }

    // 테스트용 바이패스 (Debug 빌드에서만 사용)
    fun debugLogin() {
        viewModelScope.launch {
            val testUser = com.example.nfctapapp.domain.model.User(
                id = "test-user",
                name = "테스트 사용자",
                phone = null
            )
            _authState.value = AuthState.Authenticated(testUser)
        }
    }
}
