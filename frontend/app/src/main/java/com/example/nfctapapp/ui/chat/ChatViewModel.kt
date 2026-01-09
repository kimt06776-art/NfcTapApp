package com.example.nfctapapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.remote.api.ChatApiService
import com.example.nfctapapp.data.remote.api.ChatSessionDto
import com.example.nfctapapp.data.repository.ChatRepository
import com.example.nfctapapp.data.repository.StreamResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatApiService: ChatApiService
) : ViewModel() {

    private var chatRepository: ChatRepository? = null
    private var userId: String? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun initialize(userId: String) {
        if (this.userId == userId) return
        this.userId = userId
        this.chatRepository = ChatRepository(chatApiService, userId)
        loadSessions()
    }

    fun loadSessions() {
        val repo = chatRepository ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSessions = true)

            repo.getSessions().fold(
                onSuccess = { sessions ->
                    _uiState.value = _uiState.value.copy(
                        sessions = sessions,
                        isLoadingSessions = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSessions = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun startNewChat() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            currentSessionId = null
        )
    }

    fun loadSession(sessionId: String) {
        val repo = chatRepository ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currentSessionId = sessionId)

            repo.getMessages(sessionId).fold(
                onSuccess = { messages ->
                    val chatMessages = messages.map { msg ->
                        ChatMessage(
                            id = msg.id ?: System.currentTimeMillis().toString(),
                            content = msg.content,
                            isFromUser = msg.isFromUser
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = chatMessages,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val repo = chatRepository ?: return

        android.util.Log.d("ChatViewModel", "sendMessage called: $content")
        viewModelScope.launch {
            // 세션이 없으면 새로 생성
            var sessionId = _uiState.value.currentSessionId
            if (sessionId == null) {
                val sessionResult = repo.createSession()
                sessionResult.fold(
                    onSuccess = { session ->
                        sessionId = session.id
                        _uiState.value = _uiState.value.copy(currentSessionId = session.id)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                        return@launch
                    }
                )
            }

            val currentSessionId = sessionId ?: return@launch

            // 사용자 메시지 UI에 추가
            val userMessage = ChatMessage(content = content, isFromUser = true)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + userMessage,
                isStreaming = true,
                streamingContent = "",
                error = null
            )

            // AI 응답 스트리밍으로 받기
            android.util.Log.d("ChatViewModel", "Starting stream...")
            repo.sendMessageStream(currentSessionId, content).collect { response ->
                when (response) {
                    is StreamResponse.Streaming -> {
                        android.util.Log.d("ChatViewModel", "Streaming: ${response.content.take(50)}")
                        _uiState.value = _uiState.value.copy(
                            streamingContent = response.content
                        )
                    }
                    is StreamResponse.Complete -> {
                        android.util.Log.d("ChatViewModel", "Stream complete")
                        val assistantMessage = ChatMessage(content = response.content, isFromUser = false)
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + assistantMessage,
                            isStreaming = false,
                            streamingContent = ""
                        )

                        // 첫 메시지면 제목 업데이트 & 세션 목록 갱신
                        if (_uiState.value.messages.size <= 2) {
                            val title = content.take(30) + if (content.length > 30) "..." else ""
                            repo.updateSessionTitle(currentSessionId, title)
                            loadSessions()
                        }
                    }
                    is StreamResponse.Error -> {
                        android.util.Log.e("ChatViewModel", "Stream error: ${response.message}")
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            streamingContent = "",
                            error = response.message
                        )
                    }
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val repo = chatRepository ?: return

        viewModelScope.launch {
            repo.deleteSession(sessionId).fold(
                onSuccess = {
                    // 현재 세션이면 초기화
                    if (_uiState.value.currentSessionId == sessionId) {
                        startNewChat()
                    }
                    loadSessions()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun updateSessionTitle(sessionId: String, title: String) {
        val repo = chatRepository ?: return

        viewModelScope.launch {
            repo.updateSessionTitle(sessionId, title).fold(
                onSuccess = {
                    loadSessions()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
