package com.example.nfctapapp.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nfctapapp.data.model.BibleVerse

/**
 * 성경 읽기 화면 (리팩토링 버전)
 *
 * 기능:
 * - 장별 성경 구절 표시
 * - 실시간 검색 (디바운싱 적용)
 * - 장 네비게이션
 *
 * 개선사항:
 * - Repository 패턴 적용
 * - ViewModel을 통한 상태 관리
 * - 검색 기능 추가
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreenNew(
    onBackClick: () -> Unit,
    onBookSelectorClick: () -> Unit,
    initialChapter: Int? = null,
    initialVerse: Int? = null,
    onChapterChanged: (Int) -> Unit = {},
    viewModel: BibleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(false) }

    // 초기 장/절이 지정된 경우 해당 위치로 이동
    LaunchedEffect(initialChapter, initialVerse) {
        if (initialChapter != null && initialChapter > 0) {
            if (initialVerse != null && initialVerse > 0) {
                viewModel.loadChapterAndScrollToVerse(initialChapter, initialVerse)
            } else {
                viewModel.loadChapter(initialChapter)
            }
        }
    }

    // 장이 변경되면 상위에 알림
    LaunchedEffect(uiState.currentChapter) {
        onChapterChanged(uiState.currentChapter)
    }

    // 장/절 스크롤 처리 (데이터 로드 완료 후)
    LaunchedEffect(uiState.currentChapterData, uiState.targetVerse) {
        // 데이터가 로드되었을 때만 스크롤
        val verses = uiState.currentChapterData?.verses
        if (verses != null && !uiState.isLoading) {
            val targetVerse = uiState.targetVerse
            if (targetVerse != null) {
                // 특정 절로 스크롤
                val targetIndex = targetVerse - 1
                if (targetIndex >= 0 && targetIndex < verses.size) {
                    listState.scrollToItem(targetIndex)
                    viewModel.clearTargetVerse()
                }
            }
        }
    }

    // 하이라이트가 설정되면 3초 후 자동으로 제거
    LaunchedEffect(uiState.highlightedVerse) {
        uiState.highlightedVerse?.let {
            kotlinx.coroutines.delay(3000) // 3초 대기
            viewModel.clearHighlight()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7B7A77),  // Stone Gray
                        Color(0xFF4F4E4B)   // Deep Stone
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 책 이름과 장 번호 (클릭 시 성경 선택 화면으로)
                Text(
                    text = if (isSearchBarVisible) "요한복음" else "요한복음 ${uiState.currentChapter}장",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F4F2),  // Primary Text
                    modifier = Modifier.clickable(onClick = onBookSelectorClick)
                )

                Spacer(modifier = Modifier.weight(1f))

                // 글자 크기 조절 버튼 (검색 바가 보이지 않을 때만 표시)
                if (!isSearchBarVisible) {
                    IconButton(onClick = { showFontSizeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "글자 크기",
                            tint = Color(0xFFC1BFBB)  // Tertiary Text
                        )
                    }
                }

                // 검색 모드 토글 (검색 바가 보이지 않을 때만 표시)
                if (!isSearchBarVisible) {
                    IconButton(onClick = { isSearchBarVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = Color(0xFFC1BFBB)  // Tertiary Text
                        )
                    }
                }
            }

            // 구분선
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 1.dp,
                color = Color(0xFF9A8F7A).copy(alpha = 0.3f)  // Hidden Warm
            )

            // 글자 크기 조절 다이얼로그
            if (showFontSizeDialog) {
                FontSizeDialog(
                    currentSize = uiState.textFontSize,
                    onSizeSelected = { size ->
                        viewModel.setFontSize(size)
                        showFontSizeDialog = false
                    },
                    onDismiss = { showFontSizeDialog = false }
                )
            }

            // 검색 바 (검색 바가 보일 때)
            if (isSearchBarVisible) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClearClick = {
                        isSearchBarVisible = false
                        viewModel.clearSearch()
                        keyboardController?.hide()
                    }
                )
            }

            // 콘텐츠 영역
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF5F4F2))  // Primary Text
                    }
                }

                uiState.error != null && (uiState.searchResults?.isEmpty() != false) -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "오류가 발생했습니다",
                            color = Color(0xFFD8D6D2),  // Secondary Text
                            fontSize = 14.sp
                        )
                    }
                }

                uiState.isSearchMode -> {
                    // 검색 결과 표시
                    uiState.searchResults?.let { results ->
                        SearchResults(
                            results = results,
                            searchKeyword = searchQuery,
                            onVerseClick = { verse ->
                                // 해당 구절의 장으로 이동
                                viewModel.clearSearch()
                                viewModel.loadChapter(verse.chapter)
                            }
                        )
                    }
                }

                else -> {
                    // 일반 모드: 현재 장 표시
                    uiState.currentChapterData?.let { chapter ->
                        VerseList(
                            verses = chapter.verses,
                            listState = listState,
                            highlightedVerse = uiState.highlightedVerse,
                            textFontSize = uiState.textFontSize,
                            verseNumberSize = uiState.verseNumberSize,
                            lineHeight = uiState.lineHeight,
                            onSwipeLeft = { viewModel.nextChapter() },
                            onSwipeRight = { viewModel.previousChapter() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSizeDialog(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentSize.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "글자 크기",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5F4F2)  // Primary Text
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${sliderValue.toInt()}sp",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F4F2)  // Primary Text
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 12f..30f,
                    steps = 17,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9A8F7A),  // Hidden Warm
                        activeTrackColor = Color(0xFF9A8F7A),
                        inactiveTrackColor = Color(0xFFC1BFBB).copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "12sp", fontSize = 12.sp, color = Color(0xFFC1BFBB))
                    Text(text = "30sp", fontSize = 12.sp, color = Color(0xFFC1BFBB))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSizeSelected(sliderValue.toInt())
                onDismiss()
            }) {
                Text("확인", color = Color(0xFFF5F4F2))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFFD8D6D2))
            }
        },
        containerColor = Color(0xFF4F4E4B),  // Deep Stone
        textContentColor = Color(0xFFD8D6D2),
        titleContentColor = Color(0xFFF5F4F2)
    )
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4F4E4B).copy(alpha = 0.8f)  // Deep Stone
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFFC1BFBB),  // Tertiary Text
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "검색어를 입력하세요",
                        color = Color(0xFFC1BFBB).copy(alpha = 0.7f),  // Tertiary Text
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFF5F4F2),  // Primary Text
                    unfocusedTextColor = Color(0xFFF5F4F2),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* 이미 디바운싱으로 처리됨 */ }),
                singleLine = true
            )

            if (query.isNotBlank()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "검색 닫기",
                        tint = Color(0xFFC1BFBB),  // Tertiary Text
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterNavigation(
    currentChapter: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4F4E4B).copy(alpha = 0.8f)  // Deep Stone
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClick,
                enabled = currentChapter > 1
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "이전 장",
                    tint = if (currentChapter > 1) Color(0xFFF5F4F2) else Color(0xFFC1BFBB).copy(alpha = 0.3f)
                )
            }

            Text(
                text = "${currentChapter}장",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5F4F2)  // Primary Text
            )

            IconButton(
                onClick = onNextClick,
                enabled = currentChapter < 21
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "다음 장",
                    tint = if (currentChapter < 21) Color(0xFFF5F4F2) else Color(0xFFC1BFBB).copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun ChapterSelector(
    currentChapter: Int,
    totalChapters: Int,
    onChapterSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                (1..totalChapters).forEach { chapter ->
                    val isSelected = chapter == currentChapter
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(2.dp)
                            .background(
                                color = if (isSelected) Color(0xFF9A8F7A) else Color.Transparent,  // Hidden Warm
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onChapterSelected(chapter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chapter.toString(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFF5F4F2) else Color(0xFFD8D6D2).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseList(
    verses: List<BibleVerse>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    highlightedVerse: Int? = null,
    textFontSize: Int = 18,
    verseNumberSize: Int = 14,
    lineHeight: Int = 28,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // 스와이프 임계값: 100dp
                        val threshold = 100 * density
                        when {
                            swipeOffset > threshold -> onSwipeRight() // 오른쪽으로 스와이프 (이전 장)
                            swipeOffset < -threshold -> onSwipeLeft() // 왼쪽으로 스와이프 (다음 장)
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = {
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            },
        contentPadding = PaddingValues(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(verses) { verse ->
            VerseItem(
                verseNumber = verse.verse,
                text = verse.text,
                isHighlighted = verse.verse == highlightedVerse,
                textFontSize = textFontSize,
                verseNumberSize = verseNumberSize,
                lineHeight = lineHeight
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // 이전/다음 장 네비게이션 버튼
        item {
            ChapterNavigationButtons(
                onPreviousClick = onSwipeRight,
                onNextClick = onSwipeLeft
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ChapterNavigationButtons(
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 이전 장 버튼
        TextButton(
            onClick = onPreviousClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFFD8D6D2)  // Secondary Text
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "이전 장",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "이전 장",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 다음 장 버튼
        TextButton(
            onClick = onNextClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFFD8D6D2)  // Secondary Text
            )
        ) {
            Text(
                text = "다음 장",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "다음 장",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<BibleVerse>,
    searchKeyword: String,
    onVerseClick: (BibleVerse) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "검색 결과: ${results.size}개",
                color = Color(0xFFD8D6D2),  // Secondary Text
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(results) { verse ->
            SearchResultItem(
                verse = verse,
                searchKeyword = searchKeyword,
                onClick = { onVerseClick(verse) }
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SearchResultItem(
    verse: BibleVerse,
    searchKeyword: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4F4E4B)  // Deep Stone
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "${verse.chapter}장 ${verse.verse}절",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9A8F7A)  // Hidden Warm
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = verse.text,
                fontSize = 14.sp,
                color = Color(0xFFF5F4F2),  // Primary Text
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun VerseItem(
    verseNumber: Int,
    text: String,
    isHighlighted: Boolean = false,
    textFontSize: Int = 18,
    verseNumberSize: Int = 14,
    lineHeight: Int = 28
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier
                        .background(
                            color = Color(0xFF9A8F7A).copy(alpha = 0.3f),  // Hidden Warm
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$verseNumber",
            fontSize = verseNumberSize.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color(0xFFF5F4F2) else Color(0xFF9A8F7A),  // Hidden Warm
            modifier = Modifier
                .width((verseNumberSize * 2.3).dp)
                .padding(top = 2.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = textFontSize.sp,
            color = Color(0xFFF5F4F2),  // Primary Text
            lineHeight = lineHeight.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
