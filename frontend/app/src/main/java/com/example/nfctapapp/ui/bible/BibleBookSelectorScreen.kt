package com.example.nfctapapp.ui.bible

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctapapp.data.model.BibleBooks
import com.example.nfctapapp.data.model.BookInfo
import com.example.nfctapapp.data.model.Testament

// 구약 카테고리 정의
private data class BookCategory(val name: String, val books: List<BookInfo>)

private val oldTestamentCategories = listOf(
    BookCategory("모세오경", BibleBooks.oldTestamentBooks.slice(0..4)),
    BookCategory("역사서", BibleBooks.oldTestamentBooks.slice(5..16)),
    BookCategory("시가서", BibleBooks.oldTestamentBooks.slice(17..21)),
    BookCategory("대선지서", BibleBooks.oldTestamentBooks.slice(22..26)),
    BookCategory("소선지서", BibleBooks.oldTestamentBooks.slice(27..38))
)

private val newTestamentCategories = listOf(
    BookCategory("복음서", BibleBooks.newTestamentBooks.slice(0..3)),
    BookCategory("역사서", BibleBooks.newTestamentBooks.slice(4..4)),
    BookCategory("바울서신", BibleBooks.newTestamentBooks.slice(5..17)),
    BookCategory("일반서신", BibleBooks.newTestamentBooks.slice(18..25)),
    BookCategory("예언서", BibleBooks.newTestamentBooks.slice(26..26))
)

/**
 * 성경 선택 화면
 *
 * 단계:
 * 1. 책 선택 (구약 39권 + 신약 27권 = 전체 66권)
 * 2. 장 선택
 * 3. 완료 → 해당 책의 해당 장으로 이동
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookSelectorScreen(
    currentBook: String = "요한복음",
    currentChapter: Int = 1,
    onBookSelected: (bookName: String, chapter: Int, verse: Int) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedBook by remember { mutableStateOf<BookInfo?>(null) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }

    // 시스템 뒤로가기 버튼 처리
    BackHandler {
        when {
            selectedChapter != null -> selectedChapter = null  // 절 → 장
            selectedBook != null -> selectedBook = null        // 장 → 책
            else -> onBackClick()                              // 책 → 성경 화면
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFFC1BFBB)  // Tertiary Text
                    )
                }

                Text(
                    text = "성경 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F4F2)  // Primary Text
                )
            }

            // 선택 단계 표시 (책, 장 클릭 가능)
            StepIndicator(
                currentStep = when {
                    selectedBook == null -> 1
                    selectedChapter == null -> 2
                    else -> 3
                },
                selectedBookName = selectedBook?.name,
                selectedChapter = selectedChapter,
                onBookStepClick = {
                    selectedBook = null
                    selectedChapter = null
                },
                onChapterStepClick = {
                    if (selectedBook != null) {
                        selectedChapter = null
                    }
                }
            )

            when {
                selectedBook == null -> {
                    // 책 선택 모드
                    BookSelectionView(
                        currentBook = currentBook,
                        onBookClick = { selectedBook = it }
                    )
                }
                selectedChapter == null -> {
                    // 장 선택 모드
                    ChapterSelectionView(
                        book = selectedBook!!,
                        currentChapter = if (selectedBook!!.name == currentBook) currentChapter else 1,
                        onChapterClick = { chapter ->
                            selectedChapter = chapter
                        }
                    )
                }
                else -> {
                    // 절 선택 모드
                    VerseSelectionView(
                        book = selectedBook!!,
                        chapter = selectedChapter!!,
                        onVerseClick = { verse ->
                            onBookSelected(selectedBook!!.name, selectedChapter!!, verse)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookSelectionView(
    currentBook: String,
    onBookClick: (BookInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 구약 헤더
        item {
            Text(
                text = "구약",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5F4F2),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // 구약 카테고리별 박스
        items(oldTestamentCategories) { category ->
            CategoryBox(
                category = category,
                currentBook = currentBook,
                onBookClick = onBookClick
            )
        }

        // 신약 헤더
        item {
            Text(
                text = "신약",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5F4F2),
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        // 신약 카테고리별 박스
        items(newTestamentCategories) { category ->
            CategoryBox(
                category = category,
                currentBook = currentBook,
                onBookClick = onBookClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryBox(
    category: BookCategory,
    currentBook: String,
    onBookClick: (BookInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5A5957))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 카테고리 제목
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9A8F7A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 책 목록 (1열)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                category.books.forEach { book ->
                    BookRow(
                        book = book,
                        isSelected = book.name == currentBook,
                        onClick = { onBookClick(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookRow(
    book: BookInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 약어 동그라미
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isSelected) Color(0xFF9A8F7A) else Color(0xFF4F4E4B),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.abbreviation,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color(0xFFF5F4F2) else Color(0xFFD8D6D2)
            )
        }

        // 책 이름
        Text(
            text = book.name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFF5F4F2) else Color(0xFFD8D6D2)
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    selectedBookName: String?,
    selectedChapter: Int?,
    onBookStepClick: () -> Unit,
    onChapterStepClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 책 (클릭 가능 - 현재 단계가 아닐 때)
        StepItem(
            label = "책",
            isActive = currentStep == 1,
            isCompleted = currentStep > 1,
            selectedText = selectedBookName,
            onClick = if (currentStep > 1) onBookStepClick else null
        )

        Text(text = "›", color = Color(0xFFC1BFBB).copy(alpha = 0.5f), fontSize = 20.sp)

        // 장 (클릭 가능 - 절 선택 단계일 때)
        StepItem(
            label = "장",
            isActive = currentStep == 2,
            isCompleted = currentStep > 2,
            selectedText = selectedChapter?.let { "${it}장" },
            onClick = if (currentStep > 2) onChapterStepClick else null
        )

        Text(text = "›", color = Color(0xFFC1BFBB).copy(alpha = 0.5f), fontSize = 20.sp)

        // 절 (클릭 불가)
        StepItem(
            label = "절",
            isActive = currentStep == 3,
            isCompleted = false,
            selectedText = null,
            onClick = null
        )
    }
}

@Composable
private fun RowScope.StepItem(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    selectedText: String?,
    onClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isActive -> Color(0xFFF5F4F2)  // Primary Text
                isCompleted -> Color(0xFF9A8F7A)  // Hidden Warm
                else -> Color(0xFFC1BFBB).copy(alpha = 0.5f)  // Tertiary Text
            }
        )
        if (selectedText != null) {
            Text(
                text = selectedText,
                fontSize = 12.sp,
                color = Color(0xFF9A8F7A),  // Hidden Warm
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BookCard(
    book: BookInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF9A8F7A)  // Hidden Warm
            } else {
                Color(0xFF4F4E4B)  // Deep Stone
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFF5F4F2) else Color(0xFFD8D6D2),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterSelectionView(
    book: BookInfo,
    currentChapter: Int,
    onChapterClick: (Int) -> Unit
) {
    // 장 선택 박스 (스크롤 가능)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF5A5957))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${book.name} - 장 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9A8F7A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (chapter in 1..book.totalChapters) {
                            NumberChip(
                                number = chapter,
                                isSelected = chapter == currentChapter,
                                onClick = { onChapterClick(chapter) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NumberChip(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (isSelected) Color(0xFF9A8F7A) else Color(0xFF4F4E4B),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFF5F4F2) else Color(0xFFD8D6D2)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerseSelectionView(
    book: BookInfo,
    chapter: Int,
    onVerseClick: (Int) -> Unit
) {
    // 최대 절 수 (시편 119편이 176절로 최장)
    val maxVerses = 176

    // 절 선택 박스
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF5A5957))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${book.name} ${chapter}장 - 절 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9A8F7A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (verse in 1..maxVerses) {
                            NumberChip(
                                number = verse,
                                isSelected = false,
                                onClick = { onVerseClick(verse) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
