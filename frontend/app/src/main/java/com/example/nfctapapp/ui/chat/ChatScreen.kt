package com.example.nfctapapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

data class SuggestionItem(
    val emoji: String,
    val text: String,
    val message: String
)

data class DrawerMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onMenuClick: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ViewModel 초기화
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E3A5F)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 대화 섹션
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "대화 목록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // 새 대화 버튼
                    IconButton(
                        onClick = {
                            viewModel.startNewChat()
                            scope.launch { drawerState.close() }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 대화",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 대화 세션 목록
                if (uiState.isLoadingSessions) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (uiState.sessions.isEmpty()) {
                    Text(
                        text = "저장된 대화가 없습니다",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = uiState.sessions,
                            key = { session -> session.id ?: "" }
                        ) { session ->
                            SessionItem(
                                title = session.title ?: "새 대화",
                                isSelected = session.id == uiState.currentSessionId,
                                onClick = {
                                    session.id?.let { sessionId ->
                                        viewModel.loadSession(sessionId)
                                        scope.launch { drawerState.close() }
                                    }
                                },
                                onDelete = {
                                    session.id?.let { sessionId ->
                                        viewModel.deleteSession(sessionId)
                                    }
                                },
                                onTitleUpdate = { newTitle ->
                                    session.id?.let { sessionId ->
                                        viewModel.updateSessionTitle(sessionId, newTitle)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        ChatContent(
            uiState = uiState,
            onSendMessage = { viewModel.sendMessage(it) },
            onMenuClick = { scope.launch { drawerState.open() } },
            onCloseClick = onCloseClick
        )
    }

    // 에러 처리
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 에러 표시 후 클리어
            viewModel.clearError()
        }
    }
}

@Composable
private fun SessionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTitleUpdate: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                shape = RoundedCornerShape(8.dp)
            )

            IconButton(
                onClick = {
                    if (editedTitle.isNotBlank()) {
                        onTitleUpdate(editedTitle)
                        isEditing = false
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "저장",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                maxLines = 1
            )

            IconButton(
                onClick = {
                    editedTitle = title
                    isEditing = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "편집",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val suggestions = listOf(
        SuggestionItem("😔", "힘든 일이 있어요", "요즘 힘든 일이 있어서 이야기하고 싶어요"),
        SuggestionItem("🙏", "기도 제목이 있어요", "기도 제목이 있는데 함께 나누고 싶어요"),
        SuggestionItem("📖", "말씀을 더 알고 싶어요", "성경 말씀에 대해 더 알고 싶어요")
    )

    // 새 메시지가 추가되거나 스트리밍 중일 때 자동 스크롤
    LaunchedEffect(uiState.messages.size, uiState.isStreaming, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() || uiState.isStreaming) {
            val targetIndex = uiState.messages.size + (if (uiState.isStreaming) 1 else 0)
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

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
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint = Color.White
                    )
                }

                Text(
                    text = "대화",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.White
                    )
                }
            }

            // Content Area
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                // Empty State - 추천 질문 UI
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 48.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "오늘 어떤 이야기를\n나눠볼까요?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        suggestions.forEach { suggestion ->
                            SuggestionButton(
                                suggestion = suggestion,
                                onClick = { onSendMessage(suggestion.message) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(
                        items = uiState.messages,
                        key = { message -> message.id }
                    ) { message ->
                        ChatBubble(message = message)
                    }

                    // 스트리밍 중인 메시지 표시
                    if (uiState.isStreaming) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    if (uiState.streamingContent.isEmpty()) {
                                        // 아직 응답이 시작되지 않음
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF1E3A5F)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "생각 중...",
                                                color = Color(0xFF666666),
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // 스트리밍 중인 텍스트 표시
                                        Text(
                                            text = uiState.streamingContent,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            color = Color(0xFF1E3A5F),
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("메시지를 입력하세요...", color = Color.White.copy(alpha = 0.5f))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "전송",
                        tint = Color(0xFF1E3A5F)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionButton(
    suggestion: SuggestionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = suggestion.emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = suggestion.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    Color(0xFF6B8ED6)
                else
                    Color.White.copy(alpha = 0.9f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = if (message.isFromUser) Color.White else Color(0xFF1E3A5F),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
