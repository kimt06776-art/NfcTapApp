package com.example.nfctapapp.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfctapapp.data.model.BibleChapter
import com.example.nfctapapp.data.model.BibleVerse
import com.example.nfctapapp.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 성경 화면 ViewModel
 *
 * 기능:
 * - 장별 성경 구절 표시
 * - 실시간 검색 (디바운싱 적용)
 * - 장 네비게이션
 */
@HiltViewModel
class BibleViewModel @Inject constructor(
    private val bibleRepository: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    // 검색어 (디바운싱 처리를 위한 Flow)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadChapter(1)
        setupSearch()
    }

    /**
     * 검색어 변경 시 디바운싱 적용
     * 사용자가 타이핑을 멈춘 후 300ms 후에 검색 실행
     */
    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms 디바운싱
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        // 검색어가 비어있으면 원래 장으로 복귀
                        loadChapter(_uiState.value.currentChapter)
                    }
                }
        }
    }

    /**
     * 장 로드
     */
    fun loadChapter(chapter: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val chapterData = bibleRepository.getChapter("요한복음", chapter)
                _uiState.update {
                    it.copy(
                        currentChapter = chapter,
                        currentChapterData = chapterData,
                        searchResults = null, // 검색 결과 클리어
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "데이터를 불러오는데 실패했습니다"
                    )
                }
            }
        }
    }

    /**
     * 특정 장과 절로 이동 (스크롤 포함)
     */
    fun loadChapterAndScrollToVerse(chapter: Int, verse: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val chapterData = bibleRepository.getChapter("요한복음", chapter)
                _uiState.update {
                    it.copy(
                        currentChapter = chapter,
                        currentChapterData = chapterData,
                        searchResults = null,
                        targetVerse = verse, // 스크롤할 절 설정
                        highlightedVerse = verse, // 하이라이트할 절 설정
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "데이터를 불러오는데 실패했습니다"
                    )
                }
            }
        }
    }

    /**
     * 스크롤 완료 후 타겟 절 클리어
     */
    fun clearTargetVerse() {
        _uiState.update { it.copy(targetVerse = null) }
    }

    /**
     * 하이라이트 제거
     */
    fun clearHighlight() {
        _uiState.update { it.copy(highlightedVerse = null) }
    }

    /**
     * 글자 크기 변경
     */
    fun setFontSize(size: Int) {
        _uiState.update { it.copy(textFontSize = size) }
    }

    /**
     * 검색어 업데이트
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 검색 실행
     */
    private fun performSearch(keyword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val results = bibleRepository.searchVerses(
                    bookName = "요한복음",
                    keyword = keyword,
                    ignoreCase = true
                )

                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isLoading = false,
                        error = if (results.isEmpty()) "검색 결과가 없습니다" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "검색에 실패했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 검색 모드 종료
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchResults = null, error = null) }
    }

    /**
     * 이전 장으로 이동
     */
    fun previousChapter() {
        val current = _uiState.value.currentChapter
        if (current > 1) {
            clearSearch() // 검색 모드 종료
            loadChapter(current - 1)
        }
    }

    /**
     * 다음 장으로 이동
     */
    fun nextChapter() {
        val current = _uiState.value.currentChapter
        if (current < 21) { // 요한복음은 21장까지
            clearSearch() // 검색 모드 종료
            loadChapter(current + 1)
        }
    }
}

/**
 * UI 상태 데이터 클래스
 */
data class BibleUiState(
    val currentChapter: Int = 1,
    val currentChapterData: BibleChapter? = null,
    val searchResults: List<BibleVerse>? = null, // null이면 일반 모드, 값이 있으면 검색 모드
    val targetVerse: Int? = null, // 스크롤할 절 번호 (null이면 스크롤 안 함)
    val highlightedVerse: Int? = null, // 하이라이트할 절 번호
    val textFontSize: Int = 18, // 본문 글자 크기 (sp) - 기본값 18sp
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSearchMode: Boolean
        get() = searchResults != null

    // 절 번호는 본문 크기의 약 77% (18sp -> 14sp)
    val verseNumberSize: Int
        get() = (textFontSize * 0.77f).toInt()

    // 줄 높이는 본문 크기의 약 155% (18sp -> 28sp)
    val lineHeight: Int
        get() = (textFontSize * 1.55f).toInt()
}
