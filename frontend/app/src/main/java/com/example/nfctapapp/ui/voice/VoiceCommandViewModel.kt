package com.example.nfctapapp.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.remote.api.ApiService
import com.example.nfctapapp.data.remote.api.VoiceCommandAction
import com.example.nfctapapp.data.remote.api.VoiceCommandAnalysis
import com.example.nfctapapp.data.repository.VoiceCommandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 음성 인식 상태
 */
enum class VoiceRecognitionState {
    IDLE,           // 대기 중
    LISTENING,      // 음성 인식 중
    PROCESSING,     // AI 분석 중
    SUCCESS,        // 성공
    ERROR           // 에러
}

/**
 * 음성 명령 UI 상태
 */
data class VoiceCommandUiState(
    val recognitionState: VoiceRecognitionState = VoiceRecognitionState.IDLE,
    val recognizedText: String? = null,
    val analysis: VoiceCommandAnalysis? = null,
    val error: String? = null
)

/**
 * 음성 명령 ViewModel
 *
 * 음성 인식 상태 관리 및 AI 분석 처리
 */
@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private var voiceCommandRepository: VoiceCommandRepository? = null
    private var userId: String? = null

    private val _uiState = MutableStateFlow(VoiceCommandUiState())
    val uiState: StateFlow<VoiceCommandUiState> = _uiState.asStateFlow()

    /**
     * ViewModel 초기화
     */
    fun initialize(userId: String?) {
        this.userId = userId
        this.voiceCommandRepository = VoiceCommandRepository(apiService, userId)
    }

    /**
     * 음성 인식 시작
     */
    fun startListening() {
        _uiState.value = VoiceCommandUiState(
            recognitionState = VoiceRecognitionState.LISTENING
        )
    }

    /**
     * 음성 인식 결과 처리 및 AI 분석
     */
    fun onRecognitionResult(text: String) {
        val repo = voiceCommandRepository

        if (repo == null) {
            _uiState.value = _uiState.value.copy(
                recognitionState = VoiceRecognitionState.ERROR,
                error = "ViewModel이 초기화되지 않았습니다"
            )
            return
        }

        // 인식된 텍스트 저장
        _uiState.value = _uiState.value.copy(
            recognitionState = VoiceRecognitionState.PROCESSING,
            recognizedText = text
        )

        // AI 분석 요청
        viewModelScope.launch {
            repo.analyzeCommand(text).fold(
                onSuccess = { analysis ->
                    _uiState.value = _uiState.value.copy(
                        recognitionState = VoiceRecognitionState.SUCCESS,
                        analysis = analysis,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        recognitionState = VoiceRecognitionState.ERROR,
                        error = e.message ?: "음성 명령 분석 실패"
                    )
                }
            )
        }
    }

    /**
     * 음성 인식 에러 처리
     */
    fun onRecognitionError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            recognitionState = VoiceRecognitionState.ERROR,
            error = errorMessage
        )
    }

    /**
     * 상태 초기화
     */
    fun reset() {
        _uiState.value = VoiceCommandUiState()
    }

    /**
     * 분석 결과 소비 (한 번만 실행되도록)
     */
    fun consumeAnalysis(): VoiceCommandAnalysis? {
        val analysis = _uiState.value.analysis
        _uiState.value = _uiState.value.copy(analysis = null)
        return analysis
    }
}
