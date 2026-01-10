package com.example.nfctapapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.remote.api.ChatApiService
import com.example.nfctapapp.data.remote.api.ChatSessionDto
import com.example.nfctapapp.data.remote.api.NavigationInfo
import com.example.nfctapapp.data.repository.ChatRepository
import com.example.nfctapapp.data.repository.StreamResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 메시지 유형
 */
enum class MessageType {
    TEXT  // 모든 메시지는 텍스트 (Agent가 자동으로 처리)
}

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT
)

/**
 * 네비게이션 이벤트 (Agent가 반환)
 */
data class NavigationEvent(
    val screen: String,
    val params: Map<String, String>?
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val navigationEvent: NavigationEvent? = null, // Agent 네비게이션 이벤트
    val toolsUsed: List<String> = emptyList(), // 사용된 도구 목록
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

    /**
     * 메시지 전송 (Agent 기반)
     *
     * Agent가 자동으로:
     * - 의도 파악 및 적절한 도구 사용
     * - 네비게이션 결정 (성경, 설교노트 등)
     * - 응답 생성
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val repo = chatRepository ?: return

        android.util.Log.d("ChatViewModel", "sendMessage called: $content")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            // 1. 세션 생성 (없으면)
            var sessionId = _uiState.value.currentSessionId
            if (sessionId == null) {
                val sessionResult = repo.createSession()
                sessionResult.fold(
                    onSuccess = { session ->
                        sessionId = session.id
                        _uiState.value = _uiState.value.copy(currentSessionId = session.id)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                        return@launch
                    }
                )
            }

            val currentSessionId = sessionId ?: return@launch

            // 2. 사용자 메시지 UI에 추가
            val userMessage = ChatMessage(content = content, isFromUser = true)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + userMessage
            )

            // 3. Agent 호출
            android.util.Log.d("ChatViewModel", "Calling agent...")
            repo.agentChat(currentSessionId, content).fold(
                onSuccess = { response ->
                    android.util.Log.d("ChatViewModel", "Agent response: ${response.response?.take(50)}")
                    android.util.Log.d("ChatViewModel", "Navigation: ${response.navigation}")
                    android.util.Log.d("ChatViewModel", "Tools used: ${response.toolsUsed}")

                    // AI 응답 메시지 추가
                    val assistantMessage = ChatMessage(
                        content = response.response ?: "",
                        isFromUser = false
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false,
                        toolsUsed = response.toolsUsed ?: emptyList()
                    )

                    // 네비게이션 이벤트 처리
                    response.navigation?.let { nav ->
                        _uiState.value = _uiState.value.copy(
                            navigationEvent = NavigationEvent(
                                screen = nav.screen,
                                params = nav.params
                            )
                        )
                    }

                    // 첫 메시지면 세션 제목 업데이트
                    updateSessionTitleIfFirst(repo, currentSessionId, content)
                },
                onFailure = { e ->
                    android.util.Log.e("ChatViewModel", "Agent failed: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    /**
     * 네비게이션 이벤트 소비 (네비게이션 후 호출)
     */
    fun consumeNavigationEvent() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    /**
     * 첫 메시지인 경우 세션 제목 업데이트
     */
    private suspend fun updateSessionTitleIfFirst(
        repo: ChatRepository,
        sessionId: String,
        userMessage: String
    ) {
        if (_uiState.value.messages.size <= 2) {
            val title = userMessage.take(30) + if (userMessage.length > 30) "..." else ""
            repo.updateSessionTitle(sessionId, title)
            loadSessions()
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
