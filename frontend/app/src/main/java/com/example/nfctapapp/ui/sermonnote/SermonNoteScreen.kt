package com.example.nfctapapp.ui.sermonnote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nfctapapp.data.remote.api.SermonNoteDto
import java.text.SimpleDateFormat
import java.util.*

// colors.md 색상 시스템
private val StoneGray = Color(0xFF7B7A77)
private val DeepStone = Color(0xFF4F4E4B)
private val HiddenWarm = Color(0xFF9A8F7A)
private val PrimaryText = Color(0xFFF5F4F2)
private val SecondaryText = Color(0xFFD8D6D2)
private val TertiaryText = Color(0xFFC1BFBB)
private val CardBackgroundDark = Color(0xFF5A5957)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SermonNoteScreen(
    onBackClick: () -> Unit,
    viewModel: SermonNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<SermonNoteDto?>(null) }

    // 저장 성공 시 에디터 닫기
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showEditor = false
            editingNote = null
            viewModel.clearSaveSuccess()
        }
    }

    // 에디터가 열려있을 때 시스템 뒤로가기 버튼 처리
    BackHandler(enabled = showEditor && !uiState.isSaving) {
        showEditor = false
        editingNote = null
    }

    if (showEditor) {
        SermonNoteEditor(
            note = editingNote,
            isSaving = uiState.isSaving,
            onSave = { title, content, scripture ->
                if (editingNote != null) {
                    viewModel.updateNote(
                        noteId = editingNote!!.id!!,
                        title = title,
                        content = content,
                        scripture = scripture
                    )
                } else {
                    viewModel.createNote(
                        title = title,
                        content = content,
                        scripture = scripture
                    )
                }
            },
            onBack = {
                showEditor = false
                editingNote = null
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(StoneGray, DeepStone)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // 헤더
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = PrimaryText
                            )
                        }

                        Text(
                            text = "설교 노트",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }

                    // 새 노트 작성 버튼
                    IconButton(
                        onClick = {
                            editingNote = null
                            showEditor = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 노트",
                            tint = HiddenWarm,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HiddenWarm)
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.error!!,
                                    fontSize = 14.sp,
                                    color = SecondaryText
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.loadNotes() }) {
                                    Text("다시 시도", color = HiddenWarm)
                                }
                            }
                        }
                    }
                    uiState.notes.isEmpty() -> {
                        // 빈 상태
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📝",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "작성된 설교 노트가 없습니다",
                                    fontSize = 16.sp,
                                    color = SecondaryText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "오른쪽 상단 + 버튼을 눌러\n새 노트를 작성해보세요",
                                    fontSize = 14.sp,
                                    color = TertiaryText,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                    else -> {
                        // 노트 목록
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(uiState.notes, key = { it.id ?: it.hashCode() }) { note ->
                                SermonNoteCard(
                                    note = note,
                                    onClick = {
                                        editingNote = note
                                        showEditor = true
                                    },
                                    onDelete = {
                                        note.id?.let { viewModel.deleteNote(it) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SermonNoteCard(
    note: SermonNoteDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepStone,
            title = {
                Text(
                    text = "노트 삭제",
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = "이 설교 노트를 삭제하시겠습니까?",
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("삭제", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = TertiaryText)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(note.createdAt),
                    fontSize = 12.sp,
                    color = TertiaryText
                )

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = TertiaryText.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!note.scripture.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.scripture,
                    fontSize = 13.sp,
                    color = HiddenWarm
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                fontSize = 14.sp,
                color = SecondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SermonNoteEditor(
    note: SermonNoteDto?,
    isSaving: Boolean,
    onSave: (title: String, content: String, scripture: String?) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var scripture by remember { mutableStateOf(note?.scripture ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StoneGray, DeepStone)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = PrimaryText
                        )
                    }

                    Text(
                        text = if (note != null) "노트 수정" else "새 노트",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }

                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = HiddenWarm,
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, content, scripture.ifBlank { null })
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            text = "저장",
                            fontSize = 16.sp,
                            color = if (title.isNotBlank()) PrimaryText else TertiaryText.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 제목 입력
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "설교 제목",
                            color = TertiaryText.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        cursorColor = HiddenWarm,
                        focusedBorderColor = HiddenWarm,
                        unfocusedBorderColor = TertiaryText.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    enabled = !isSaving,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 성경 구절 입력
                OutlinedTextField(
                    value = scripture,
                    onValueChange = { scripture = it },
                    placeholder = {
                        Text(
                            text = "본문 말씀 (예: 요한복음 3:16)",
                            color = TertiaryText.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SecondaryText,
                        unfocusedTextColor = SecondaryText,
                        cursorColor = SecondaryText,
                        focusedBorderColor = TertiaryText.copy(alpha = 0.5f),
                        unfocusedBorderColor = TertiaryText.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    enabled = !isSaving,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 내용 입력
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "설교 내용을 기록하세요...",
                            color = TertiaryText.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SecondaryText,
                        unfocusedTextColor = SecondaryText,
                        cursorColor = HiddenWarm,
                        focusedBorderColor = HiddenWarm,
                        unfocusedBorderColor = TertiaryText.copy(alpha = 0.3f)
                    ),
                    enabled = !isSaving,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun formatDate(isoDate: String?): String {
    if (isoDate.isNullOrEmpty()) return ""

    return try {
        // ISO 8601 형식 파싱 (예: 2025-01-09T12:30:00Z)
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val date = inputFormat.parse(isoDate.substring(0, 19))
        date?.let { outputFormat.format(it) } ?: isoDate.substring(0, 10)
    } catch (e: Exception) {
        // 파싱 실패 시 원본 날짜의 앞 10자리 반환
        isoDate.take(10)
    }
}
