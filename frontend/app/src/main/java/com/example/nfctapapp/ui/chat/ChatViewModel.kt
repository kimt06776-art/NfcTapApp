package com.example.nfctapapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.remote.api.BibleReadingData
import com.example.nfctapapp.data.remote.api.BibleStudyData
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

/**
 * 메시지 유형
 */
enum class MessageType {
    TEXT,           // 일반 텍스트 메시지
    BIBLE_STUDY     // 구조화된 성경 공부 응답
}

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val bibleStudyData: BibleStudyData? = null
)

/**
 * 성경 네비게이션 이벤트
 */
data class BibleNavigationEvent(
    val book: String,
    val chapter: Int,
    val verse: Int? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isClassifying: Boolean = false,  // Intent 분류 중
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val currentIntent: String? = null,   // 현재 분류된 intent
    val bibleNavigationEvent: BibleNavigationEvent? = null, // 성경 네비게이션 이벤트
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
            // 1. 먼저 Intent 분류 (메시지 추가 전에)
            _uiState.value = _uiState.value.copy(
                isClassifying = true,
                error = null
            )

            android.util.Log.d("ChatViewModel", "Classifying intent...")
            val intentResult = repo.classifyIntent(content)

            val intent = intentResult.fold(
                onSuccess = { classification ->
                    android.util.Log.d("ChatViewModel", "Intent: ${classification.intent} (confidence: ${classification.confidence})")
                    classification.intent
                },
                onFailure = { e ->
                    android.util.Log.e("ChatViewModel", "Intent classification failed: ${e.message}")
                    "general" // 분류 실패 시 기본값
                }
            )

            _uiState.value = _uiState.value.copy(
                isClassifying = false,
                currentIntent = intent
            )

            // 2. bible_reading이면 채팅에 메시지 추가 없이 바로 네비게이션
            if (intent == "bible_reading") {
                handleBibleReading(repo, content)
                return@launch
            }

            // 3. 다른 intent는 세션 생성 및 메시지 추가 후 처리
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
                messages = _uiState.value.messages + userMessage
            )

            // Intent에 따라 라우팅
            when (intent) {
                "bible_study" -> {
                    // 성경 공부 → 구조화된 응답
                    handleBibleStudy(repo, currentSessionId, content)
                }
                else -> {
                    // 상담, 기도, 일반 → 스트리밍 응답
                    handleStreamingResponse(repo, currentSessionId, content)
                }
            }
        }
    }

    /**
     * 성경 읽기 요청 처리 (네비게이션)
     * 성공: 채팅에 메시지 추가 없이 바로 성경 화면으로 이동
     * 실패: 사용자 메시지 + AI 응답을 채팅에 표시
     */
    private suspend fun handleBibleReading(
        repo: ChatRepository,
        userMessage: String
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        repo.getBibleReading(userMessage).fold(
            onSuccess = { bibleReadingData ->
                android.util.Log.d("ChatViewModel", "Bible reading parsed: ${bibleReadingData.book} ${bibleReadingData.chapter}:${bibleReadingData.verse}")

                // 바로 네비게이션 이벤트 발생 (채팅에 메시지 추가 안 함)
                // chapter는 Repository에서 null 체크 완료됨
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bibleNavigationEvent = BibleNavigationEvent(
                        book = bibleReadingData.book,
                        chapter = bibleReadingData.chapter!!,
                        verse = bibleReadingData.verse
                    )
                )
            },
            onFailure = { e ->
                android.util.Log.e("ChatViewModel", "Bible reading parse failed: ${e.message}")

                // 실패 시 사용자 메시지와 AI 응답을 채팅에 표시
                // 백엔드에서 받은 에러 메시지를 그대로 사용
                val userChatMessage = ChatMessage(content = userMessage, isFromUser = true)
                val aiResponse = ChatMessage(
                    content = e.message ?: "요청하신 성경책을 찾을 수 없어요. 다시 시도해주세요.",
                    isFromUser = false
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + userChatMessage + aiResponse,
                    isLoading = false
                )
            }
        )
    }

    /**
     * 성경 네비게이션 이벤트 소비 (네비게이션 후 호출)
     */
    fun consumeBibleNavigationEvent() {
        _uiState.value = _uiState.value.copy(bibleNavigationEvent = null)
    }

    /**
     * 성경 공부 응답 처리 (구조화된 JSON)
     */
    private suspend fun handleBibleStudy(
        repo: ChatRepository,
        sessionId: String,
        userMessage: String
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        repo.getBibleStudy(sessionId, userMessage).fold(
            onSuccess = { bibleStudyData ->
                android.util.Log.d("ChatViewModel", "Bible study response received")

                // 구조화된 성경 공부 메시지 추가
                val assistantMessage = ChatMessage(
                    content = bibleStudyData.passageExplanation, // 기본 표시 내용
                    isFromUser = false,
                    type = MessageType.BIBLE_STUDY,
                    bibleStudyData = bibleStudyData
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )

                // 첫 메시지면 제목 업데이트
                updateSessionTitleIfFirst(repo, sessionId, userMessage)
            },
            onFailure = { e ->
                android.util.Log.e("ChatViewModel", "Bible study failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        )
    }

    /**
     * 스트리밍 응답 처리 (상담, 기도, 일반)
     */
    private suspend fun handleStreamingResponse(
        repo: ChatRepository,
        sessionId: String,
        userMessage: String
    ) {
        _uiState.value = _uiState.value.copy(
            isStreaming = true,
            streamingContent = ""
        )

        android.util.Log.d("ChatViewModel", "Starting stream...")
        repo.sendMessageStream(sessionId, userMessage).collect { response ->
            when (response) {
                is StreamResponse.Streaming -> {
                    android.util.Log.d("ChatViewModel", "Streaming: ${response.content.take(50)}")
                    _uiState.value = _uiState.value.copy(
                        streamingContent = response.content
                    )
                }
                is StreamResponse.Complete -> {
                    android.util.Log.d("ChatViewModel", "Stream complete")
                    val assistantMessage = ChatMessage(
                        content = response.content,
                        isFromUser = false,
                        type = MessageType.TEXT
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isStreaming = false,
                        streamingContent = ""
                    )

                    updateSessionTitleIfFirst(repo, sessionId, userMessage)
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
