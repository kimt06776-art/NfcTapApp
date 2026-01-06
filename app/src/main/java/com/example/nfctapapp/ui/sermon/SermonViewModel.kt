package com.example.nfctapapp.ui.sermon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.Sermon
import com.example.nfctapapp.data.SermonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SermonUiState(
    val isLoading: Boolean = true,
    val sermons: List<Sermon> = emptyList(),
    val latestSermon: Sermon? = null,
    val error: String? = null
)

@HiltViewModel
class SermonViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SermonUiState())
    val uiState: StateFlow<SermonUiState> = _uiState.asStateFlow()

    init {
        loadSermons()
    }

    fun loadSermons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = SermonRepository.getAllSermons()

            result.fold(
                onSuccess = { sermons ->
                    _uiState.value = SermonUiState(
                        isLoading = false,
                        sermons = sermons,
                        latestSermon = sermons.firstOrNull(),
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = SermonUiState(
                        isLoading = false,
                        error = e.message ?: "설교 목록을 불러올 수 없습니다"
                    )
                }
            )
        }
    }

    fun refresh() {
        loadSermons()
    }
}
