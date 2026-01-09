package com.example.nfctapapp.ui.sermonnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.local.UserPreferences
import com.example.nfctapapp.data.remote.api.SermonNoteApiService
import com.example.nfctapapp.data.remote.api.SermonNoteCreateRequest
import com.example.nfctapapp.data.remote.api.SermonNoteDto
import com.example.nfctapapp.data.remote.api.SermonNoteUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SermonNoteUiState(
    val isLoading: Boolean = false,
    val notes: List<SermonNoteDto> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SermonNoteViewModel @Inject constructor(
    private val sermonNoteApiService: SermonNoteApiService,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SermonNoteUiState())
    val uiState: StateFlow<SermonNoteUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    init {
        loadUserId()
    }

    private fun loadUserId() {
        viewModelScope.launch {
            val user = userPreferences.cachedUser.first()
            userId = user?.id
            if (userId != null) {
                loadNotes()
            }
        }
    }

    fun loadNotes() {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = sermonNoteApiService.getSermonNotes(currentUserId)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            notes = body.notes ?: emptyList(),
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = body?.error ?: "노트를 불러올 수 없습니다"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "서버 오류: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "네트워크 오류"
                )
            }
        }
    }

    fun createNote(title: String, content: String, scripture: String?) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false)

            try {
                val response = sermonNoteApiService.createSermonNote(
                    SermonNoteCreateRequest(
                        userId = currentUserId,
                        title = title,
                        content = content,
                        scripture = scripture
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    loadNotes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = response.body()?.error ?: "저장 실패"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "네트워크 오류"
                )
            }
        }
    }

    fun updateNote(noteId: String, title: String?, content: String?, scripture: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false)

            try {
                val response = sermonNoteApiService.updateSermonNote(
                    noteId = noteId,
                    request = SermonNoteUpdateRequest(
                        title = title,
                        content = content,
                        scripture = scripture
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    loadNotes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = response.body()?.error ?: "수정 실패"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "네트워크 오류"
                )
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                val response = sermonNoteApiService.deleteSermonNote(noteId)

                if (response.isSuccessful && response.body()?.success == true) {
                    loadNotes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = response.body()?.error ?: "삭제 실패"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "네트워크 오류"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
