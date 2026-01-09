package com.example.nfctapapp.ui.chat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// Color System (from colors.md)
private object ChatColors {
    val StoneGray = Color(0xFF7B7A77)      // Background
    val DeepStone = Color(0xFF4F4E4B)      // Depth/Ground
    val HiddenWarm = Color(0xFF9A8F7A)     // Subtle accent
    val PrimaryText = Color(0xFFF5F4F2)    // Main text
    val SecondaryText = Color(0xFFD8D6D2)  // Supporting text
    val TertiaryText = Color(0xFFC1BFBB)   // UI elements
}

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
                drawerContainerColor = ChatColors.DeepStone
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
                        color = ChatColors.PrimaryText
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
                            tint = ChatColors.PrimaryText
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
                            color = ChatColors.PrimaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (uiState.sessions.isEmpty()) {
                    Text(
                        text = "저장된 대화가 없습니다",
                        color = ChatColors.SecondaryText,
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
                if (isSelected) ChatColors.StoneGray.copy(alpha = 0.5f) else Color.Transparent
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
                    focusedTextColor = ChatColors.PrimaryText,
                    unfocusedTextColor = ChatColors.PrimaryText,
                    focusedBorderColor = ChatColors.HiddenWarm,
                    unfocusedBorderColor = ChatColors.TertiaryText,
                    cursorColor = ChatColors.PrimaryText
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
                    tint = ChatColors.SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Text(
                text = title,
                color = ChatColors.PrimaryText,
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
                    tint = ChatColors.TertiaryText,
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
                    tint = ChatColors.TertiaryText,
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
    var isListening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 음성 인식기
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // 음성 인식 리스너
    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    inputText = matches[0]
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    inputText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(speechRecognizer)
        }
    }


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
                        ChatColors.StoneGray,
                        ChatColors.DeepStone
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
                        tint = ChatColors.TertiaryText
                    )
                }

                Text(
                    text = "누아와 대화",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChatColors.PrimaryText
                )

                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = ChatColors.TertiaryText
                    )
                }
            }

            // Content Area
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                // Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "무엇이든 물어보세요",
                        fontSize = 18.sp,
                        color = ChatColors.SecondaryText,
                        textAlign = TextAlign.Center
                    )
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
                                        containerColor = ChatColors.DeepStone.copy(alpha = 0.8f)
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
                                                color = ChatColors.SecondaryText
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "생각 중...",
                                                color = ChatColors.SecondaryText,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // 스트리밍 중인 텍스트 표시
                                        Text(
                                            text = uiState.streamingContent,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            color = ChatColors.PrimaryText,
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
                    .background(ChatColors.DeepStone)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (isListening) "듣고 있어요..." else "메시지를 입력하세요...",
                            color = if (isListening) ChatColors.HiddenWarm else ChatColors.TertiaryText
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ChatColors.PrimaryText,
                        unfocusedTextColor = ChatColors.PrimaryText,
                        focusedBorderColor = if (isListening) ChatColors.HiddenWarm else ChatColors.HiddenWarm,
                        unfocusedBorderColor = if (isListening) ChatColors.HiddenWarm else ChatColors.StoneGray,
                        cursorColor = ChatColors.PrimaryText,
                        focusedContainerColor = ChatColors.StoneGray.copy(alpha = 0.3f),
                        unfocusedContainerColor = ChatColors.StoneGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 마이크 버튼
                IconButton(
                    onClick = {
                        if (isListening) {
                            speechRecognizer.stopListening()
                            isListening = false
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) ChatColors.HiddenWarm.copy(alpha = 0.8f)
                            else ChatColors.StoneGray.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "음성 입력",
                        tint = if (isListening) ChatColors.PrimaryText else ChatColors.SecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 전송 버튼
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
                        .background(ChatColors.HiddenWarm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "전송",
                        tint = ChatColors.PrimaryText
                    )
                }
            }
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
                    ChatColors.HiddenWarm
                else
                    ChatColors.DeepStone.copy(alpha = 0.8f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = ChatColors.PrimaryText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private fun startSpeechRecognition(speechRecognizer: SpeechRecognizer) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toString())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    speechRecognizer.startListening(intent)
}
