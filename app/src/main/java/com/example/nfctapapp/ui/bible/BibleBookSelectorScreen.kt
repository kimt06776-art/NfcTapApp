package com.example.nfctapapp.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctapapp.data.model.BibleBooks
import com.example.nfctapapp.data.model.BookInfo
import com.example.nfctapapp.data.model.Testament

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A5F),
                        Color(0xFF2D5478),
                        Color(0xFF3D6E91)
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
                        tint = Color.White
                    )
                }

                Text(
                    text = "성경 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 선택 단계 표시
            StepIndicator(
                currentStep = when {
                    selectedBook == null -> 1
                    selectedChapter == null -> 2
                    else -> 3
                },
                selectedBookName = selectedBook?.name,
                selectedChapter = selectedChapter
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
                        },
                        onBackToBooks = {
                            selectedBook = null
                            selectedChapter = null
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
                        },
                        onBackToChapters = { selectedChapter = null }
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 구약 헤더
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Text(
                text = "구약 (39권)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        // 구약 39권
        items(BibleBooks.oldTestamentBooks) { book ->
            BookCard(
                book = book,
                isSelected = book.name == currentBook,
                onClick = { onBookClick(book) }
            )
        }

        // 신약 헤더
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Text(
                text = "신약 (27권)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )
        }

        // 신약 27권
        items(BibleBooks.newTestamentBooks) { book ->
            BookCard(
                book = book,
                isSelected = book.name == currentBook,
                onClick = { onBookClick(book) }
            )
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    selectedBookName: String?,
    selectedChapter: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 책
        StepItem(
            label = "책",
            isActive = currentStep == 1,
            isCompleted = currentStep > 1,
            selectedText = selectedBookName
        )

        Text(text = "›", color = Color.White.copy(alpha = 0.5f), fontSize = 20.sp)

        // 장
        StepItem(
            label = "장",
            isActive = currentStep == 2,
            isCompleted = currentStep > 2,
            selectedText = selectedChapter?.let { "${it}장" }
        )

        Text(text = "›", color = Color.White.copy(alpha = 0.5f), fontSize = 20.sp)

        // 절
        StepItem(
            label = "절",
            isActive = currentStep == 3,
            isCompleted = false,
            selectedText = null
        )
    }
}

@Composable
private fun RowScope.StepItem(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    selectedText: String?
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isActive -> Color.White
                isCompleted -> Color(0xFF6B8ED6)
                else -> Color.White.copy(alpha = 0.5f)
            }
        )
        if (selectedText != null) {
            Text(
                text = selectedText,
                fontSize = 12.sp,
                color = Color(0xFF6B8ED6),
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
                Color(0xFF6B8ED6)
            } else {
                Color.White.copy(alpha = 0.95f)
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
                color = if (isSelected) Color.White else Color(0xFF1E3A5F),
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChapterSelectionView(
    book: BookInfo,
    currentChapter: Int,
    onChapterClick: (Int) -> Unit,
    onBackToBooks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 뒤로 버튼
        TextButton(
            onClick = onBackToBooks,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Text("← 책 선택으로 돌아가기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 장 선택 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(book.totalChapters) { index ->
                val chapter = index + 1
                ChapterCard(
                    chapter = chapter,
                    isSelected = chapter == currentChapter,
                    onClick = { onChapterClick(chapter) }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF6B8ED6)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chapter.toString(),
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF1E3A5F)
            )
        }
    }
}

@Composable
private fun VerseSelectionView(
    book: BookInfo,
    chapter: Int,
    onVerseClick: (Int) -> Unit,
    onBackToChapters: () -> Unit
) {
    // 최대 절 수: 일반적으로 150절 정도면 충분 (시편 119편이 176절로 최장)
    val maxVerses = 176

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 뒤로 버튼
        TextButton(
            onClick = onBackToChapters,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Text("← 장 선택으로 돌아가기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 절 선택 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(maxVerses) { index ->
                val verse = index + 1
                VerseCard(
                    verse = verse,
                    onClick = { onVerseClick(verse) }
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun VerseCard(
    verse: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = verse.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1E3A5F)
            )
        }
    }
}
