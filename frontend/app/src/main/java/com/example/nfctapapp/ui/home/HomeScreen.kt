package com.example.nfctapapp.ui.home

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nfctapapp.ui.chat.ChatViewModel
import com.example.nfctapapp.ui.chat.ChatMessage
import com.example.nfctapapp.ui.voice.LiquidBlobAnimation
import kotlinx.coroutines.launch

// colors.md 색상 시스템
private val StoneGray = Color(0xFF7B7A77)
private val DeepStone = Color(0xFF4F4E4B)
private val HiddenWarm = Color(0xFF9A8F7A)
private val PrimaryText = Color(0xFFF5F4F2)
private val SecondaryText = Color(0xFFD8D6D2)
private val TertiaryText = Color(0xFFC1BFBB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String = "",
    onMenuClick: (String) -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showDrawer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Chat state
    val uiState by chatViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // ViewModel 초기화
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            chatViewModel.initialize(userId)
        }
    }

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
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                    chatViewModel.sendMessage(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(speechRecognizer, context)
        }
    }

    // 새 메시지가 추가되면 자동 스크롤
    LaunchedEffect(uiState.messages.size, uiState.isStreaming, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() || uiState.isStreaming) {
            val targetIndex = uiState.messages.size + (if (uiState.isStreaming) 1 else 0)
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Agent 네비게이션 이벤트 처리
    LaunchedEffect(uiState.navigationEvent) {
        uiState.navigationEvent?.let { event ->
            android.util.Log.d("HomeScreen", "Navigation event: ${event.screen}, params: ${event.params}")

            when (event.screen) {
                "bible" -> {
                    // 성경 화면으로 이동
                    val chapter = event.params?.get("chapter")?.toIntOrNull() ?: 1
                    val verse = event.params?.get("verse")?.toIntOrNull() ?: -1
                    onMenuClick("bible?chapter=$chapter&verse=$verse")
                }
                "sermon_note" -> {
                    // 설교 노트 화면으로 이동
                    onMenuClick("sermonNote")
                }
                "today_verse" -> {
                    // 오늘의 말씀 화면
                    onMenuClick("todayVerse")
                }
                "sermon" -> {
                    // 설교 화면
                    onMenuClick("sermon")
                }
                "community" -> {
                    // 공동체 화면
                    onMenuClick("community")
                }
                else -> {
                    android.util.Log.w("HomeScreen", "Unknown screen: ${event.screen}")
                }
            }
            chatViewModel.consumeNavigationEvent()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StoneGray, DeepStone)
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 상단 바 (햄버거 메뉴만)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // 알림 아이콘
                IconButton(onClick = { /* TODO: 알림 화면으로 이동 */ }) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "알림",
                        tint = PrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 메뉴 아이콘
                IconButton(onClick = { showDrawer = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "메뉴",
                        tint = PrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 채팅 메시지 영역
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                // Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .offset(y = (-80).dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 마이크 모드가 아닐 때만 텍스트 표시
                    if (!isListening) {
                        Text(
                            text = "오늘의 신앙 이야기를 나눠보아요",
                            fontSize = 18.sp,
                            color = SecondaryText
                        )
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

                    // Agent 처리 중 표시
                    if (uiState.isLoading) {
                        item {
                            LoadingBubble(text = "생각 중...")
                        }
                    }

                    // 스트리밍 중인 메시지 표시
                    if (uiState.isStreaming) {
                        item {
                            StreamingBubble(
                                content = uiState.streamingContent,
                                isWaiting = uiState.streamingContent.isEmpty()
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // 입력 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(DeepStone)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "무엇이든지 좋아요",
                            color = TertiaryText
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HiddenWarm,
                        unfocusedBorderColor = StoneGray,
                        cursorColor = PrimaryText,
                        focusedContainerColor = StoneGray.copy(alpha = 0.3f),
                        unfocusedContainerColor = StoneGray.copy(alpha = 0.3f)
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
                            if (isListening) HiddenWarm.copy(alpha = 0.8f)
                            else StoneGray.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "음성 입력",
                        tint = if (isListening) PrimaryText else SecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 전송 버튼
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HiddenWarm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "전송",
                        tint = PrimaryText
                    )
                }
            }
        }

        // 화면 중앙 액체 방울 애니메이션 (음성 인식 중)
        if (isListening) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-100).dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidBlobAnimation(
                    modifier = Modifier.size(1000.dp),
                    isAnimating = true,
                    color = HiddenWarm
                )
            }
        }

        // 오른쪽에서 나오는 메뉴 드로어
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 배경 (클릭하면 닫힘)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showDrawer = false }
                )

                // 드로어 내용 - 전체 스크롤 가능
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(DeepStone)
                        .padding(top = 48.dp)
                ) {
                    // 헤더
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "메뉴",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            IconButton(
                                onClick = {
                                    showDrawer = false
                                    // TODO: 설정 화면으로 이동
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "설정",
                                    tint = TertiaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = TertiaryText.copy(alpha = 0.2f))
                    }

                    item {
                        MenuSheetItem(
                            icon = Icons.Rounded.Book,
                            title = "말씀",
                            onClick = {
                                showDrawer = false
                                onMenuClick("bible")
                            }
                        )
                    }

                    item {
                        MenuSheetItem(
                            icon = Icons.Rounded.Campaign,
                            title = "공동체",
                            onClick = {
                                showDrawer = false
                                onMenuClick("community")
                            }
                        )
                    }

                    item {
                        MenuSheetItem(
                            icon = Icons.Outlined.Description,
                            title = "설교 노트",
                            onClick = {
                                showDrawer = false
                                onMenuClick("sermonNote")
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = TertiaryText.copy(alpha = 0.2f))
                    }

                    // 대화 기록 섹션 헤더
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "대화 기록",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )

                            IconButton(
                                onClick = {
                                    chatViewModel.startNewChat()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "새 대화",
                                    tint = TertiaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 채팅 세션 목록
                    if (uiState.isLoadingSessions) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = TertiaryText,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } else if (uiState.sessions.isEmpty()) {
                        item {
                            Text(
                                text = "저장된 대화가 없습니다",
                                color = TertiaryText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(
                            items = uiState.sessions,
                            key = { session -> session.id ?: "" }
                        ) { session ->
                            ChatSessionItem(
                                title = session.title ?: "새 대화",
                                isSelected = session.id == uiState.currentSessionId,
                                onClick = {
                                    session.id?.let { sessionId ->
                                        chatViewModel.loadSession(sessionId)
                                        showDrawer = false
                                    }
                                },
                                onDelete = {
                                    session.id?.let { sessionId ->
                                        chatViewModel.deleteSession(sessionId)
                                    }
                                },
                                onTitleUpdate = { newTitle ->
                                    session.id?.let { sessionId ->
                                        chatViewModel.updateSessionTitle(sessionId, newTitle)
                                    }
                                }
                            )
                        }
                    }
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
                containerColor = if (message.isFromUser) HiddenWarm else DeepStone.copy(alpha = 0.8f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = PrimaryText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun LoadingBubble(text: String) {
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
                containerColor = DeepStone.copy(alpha = 0.8f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(content: String, isWaiting: Boolean) {
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
                containerColor = DeepStone.copy(alpha = 0.8f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (isWaiting) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "생각 중...",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = content,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = PrimaryText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun MenuSheetItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TertiaryText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = PrimaryText
        )
    }
}

@Composable
private fun ChatSessionItem(
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
                if (isSelected) StoneGray.copy(alpha = 0.3f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = HiddenWarm,
                    unfocusedBorderColor = TertiaryText,
                    cursorColor = PrimaryText
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            IconButton(
                onClick = {
                    if (editedTitle.isNotBlank()) {
                        onTitleUpdate(editedTitle)
                        isEditing = false
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "저장",
                    tint = HiddenWarm,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Text(
                text = title,
                color = if (isSelected) PrimaryText else SecondaryText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            IconButton(
                onClick = {
                    editedTitle = title
                    isEditing = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "수정",
                    tint = TertiaryText,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = TertiaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun startSpeechRecognition(speechRecognizer: SpeechRecognizer, context: android.content.Context) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    speechRecognizer.startListening(intent)
}
