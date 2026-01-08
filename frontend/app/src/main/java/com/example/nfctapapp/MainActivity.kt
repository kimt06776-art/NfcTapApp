package com.example.nfctapapp

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nfctapapp.data.MeditationRepository
import com.example.nfctapapp.data.NoticeRepository
import com.example.nfctapapp.data.SampleSermonData
import com.example.nfctapapp.data.Sermon
import com.example.nfctapapp.data.VerseRepository
import com.example.nfctapapp.ui.sermon.SermonViewModel
import com.example.nfctapapp.domain.model.AuthState
import com.example.nfctapapp.ui.auth.AuthViewModel
import com.example.nfctapapp.ui.auth.RegistrationScreen
import com.example.nfctapapp.ui.auth.WelcomeScreen
import com.example.nfctapapp.ui.chat.ChatScreen
import com.example.nfctapapp.ui.chat.ChatViewModel
import com.example.nfctapapp.ui.theme.NfcTapAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    // NFC 이벤트 전달용
    private val nfcTagUid = mutableStateOf<String?>(null)
    private var initialNfcUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupNfc()
        handleIntent(intent)
        initialNfcUid = nfcTagUid.value  // handleIntent 후 초기값 캡처

        setContent {
            NfcTapAppTheme {
                val nfcUid by nfcTagUid
                val capturedInitialUid = remember { initialNfcUid }

                AppNavigation(
                    nfcTagUid = nfcUid,
                    initialNfcUid = capturedInitialUid,
                    onNfcConsumed = {
                        nfcTagUid.value = null
                        initialNfcUid = null
                    }
                )
            }
        }
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "이 기기는 NFC를 지원하지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                // NDEF 메시지에서 URL 읽기
                val ndefUrl = readNdefUrl(intent)
                Log.d("NFC", "NDEF URL: $ndefUrl")

                // 우리 앱 전용 URL인지 확인 (nfctap://auth 또는 파라미터 포함)
                if (ndefUrl?.startsWith("nfctap://auth") == true) {
                    // 우리 앱 NFC 태그 확인됨 → UID 읽기
                    val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    }

                    tag?.let { nfcTag ->
                        val uid = nfcTag.id.toHexString()
                        Log.d("NFC", "Tag UID: $uid")
                        nfcTagUid.value = uid
                        Toast.makeText(this, "NFC 인증 중...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 다른 앱의 NFC 태그 → 무시
                    Log.d("NFC", "다른 앱의 NFC 태그: $ndefUrl")
                    Toast.makeText(this, "이 NFC 태그는 지원하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_VIEW -> {
                // 딥링크로 실행된 경우 (nfctap://auth)
                val data = intent.data
                if (data?.scheme == "nfctap" && data.host == "auth") {
                    Log.d("NFC", "딥링크로 실행됨: $data")
                    // 딥링크만으로는 UID를 알 수 없으므로 NFC 태그 터치 안내
                    Toast.makeText(this, "NFC 태그를 터치해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * NDEF 메시지에서 URL 추출
     */
    private fun readNdefUrl(intent: Intent): String? {
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        rawMessages?.let { messages ->
            for (message in messages) {
                if (message is NdefMessage) {
                    for (record in message.records) {
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_URI)) {
                            // URI 레코드 파싱
                            return parseUriRecord(record)
                        } else if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                            // 절대 URI
                            return String(record.type, Charsets.UTF_8)
                        }
                    }
                }
            }
        }

        // Intent의 data URI도 확인 (NDEF_DISCOVERED 인텐트의 경우)
        return intent.data?.toString()
    }

    /**
     * NdefRecord에서 URI 파싱
     * RTD_URI는 첫 바이트가 URI 프리픽스 코드
     */
    private fun parseUriRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return ""

        val prefixCode = payload[0].toInt() and 0xFF
        val prefix = when (prefixCode) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            else -> ""
        }

        val uriPart = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return prefix + uriPart
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}

@Composable
fun AppNavigation(
    nfcTagUid: String?,
    initialNfcUid: String?,
    onNfcConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // 초기화 처리: 앱 시작 시 한 번만 실행
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasInitialized) {
            hasInitialized = true
            if (initialNfcUid != null) {
                // NFC로 앱 시작됨 → 인증 시도
                authViewModel.onNfcTagScanned(initialNfcUid)
                onNfcConsumed()
            } else {
                // 일반 시작 → 캐시 체크
                authViewModel.checkCachedUser()
            }
        }
    }

    // 앱 실행 중 NFC 태그 감지 시 처리
    LaunchedEffect(nfcTagUid) {
        if (hasInitialized && nfcTagUid != null) {
            authViewModel.onNfcTagScanned(nfcTagUid)
            onNfcConsumed()
        }
    }

    // 인증 상태에 따른 화면 전환 (splash에서 다른 화면으로)
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.NfcNotRegistered -> {
                navController.navigate("registration") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {
                // Initial, Loading, NfcScanned: splash 화면 유지
            }
        }
    }

    // 항상 splash에서 시작
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen()
        }

        composable("welcome") {
            WelcomeScreen(
                onTestLogin = { authViewModel.debugLogin() }
            )
        }

        composable("registration") {
            val isLoading = authState is AuthState.Loading
            RegistrationScreen(
                isLoading = isLoading,
                onRegister = { name, phone ->
                    authViewModel.registerUser(name, phone)
                }
            )
        }

        composable("home") {
            HomeScreen(
                onMenuClick = { menuId ->
                    navController.navigate(menuId)
                },
                onChatClick = { navController.navigate("chat") }
            )
        }

        composable("todayVerse") {
            TodayVerseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("chat") {
            // 인증된 사용자의 ID 가져오기
            val userId = (authState as? AuthState.Authenticated)?.user?.id ?: ""

            ChatScreen(
                userId = userId,
                onMenuClick = { menuId ->
                    when (menuId) {
                        "sermon" -> navController.navigate("sermon")
                        "bible" -> navController.navigate("bible")
                        "notice" -> navController.navigate("notice")
                        "community" -> navController.navigate("community")
                        "worship" -> navController.navigate("worship")
                        "gamification" -> navController.navigate("gamification")
                    }
                },
                onCloseClick = { navController.popBackStack() }
            )
        }

        composable("sermon") {
            val sermonViewModel: SermonViewModel = hiltViewModel()
            SermonScreen(
                viewModel = sermonViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "bible?chapter={chapter}&verse={verse}",
            arguments = listOf(
                navArgument("chapter") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("verse") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val chapter = backStackEntry.arguments?.getInt("chapter") ?: -1
            val verse = backStackEntry.arguments?.getInt("verse") ?: -1

            com.example.nfctapapp.ui.bible.BibleScreenNew(
                onBackClick = { navController.popBackStack() },
                onBookSelectorClick = { navController.navigate("bibleBookSelector") },
                initialChapter = if (chapter > 0) chapter else null,
                initialVerse = if (verse > 0) verse else null
            )
        }

        composable("bibleBookSelector") {
            val context = LocalContext.current
            com.example.nfctapapp.ui.bible.BibleBookSelectorScreen(
                currentBook = "요한복음",
                currentChapter = 1,
                onBookSelected = { bookName, chapter, verse ->
                    // 선택된 책, 장, 절로 이동
                    if (bookName == "요한복음") {
                        // bible 화면으로 이동하면서 장과 절 파라미터 전달
                        navController.navigate("bible?chapter=$chapter&verse=$verse") {
                            popUpTo("bibleBookSelector") { inclusive = true }
                        }
                    } else {
                        // 다른 책은 아직 데이터가 없음을 알림
                        Toast.makeText(
                            context,
                            "$bookName ${chapter}장 ${verse}절 데이터는 아직 준비 중입니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("gamification") {
            com.example.nfctapapp.ui.gamification.GamificationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("notice") {
            com.example.nfctapapp.ui.notice.NoticeScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("community") {
            com.example.nfctapapp.ui.community.CommunityScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("worship") {
            com.example.nfctapapp.ui.worship.WorshipScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeHeader(
    onSearchClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 로고 이미지
                Image(
                    painter = painterResource(id = R.drawable.logo_nfc),
                    contentDescription = "NFC 복음 로고",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 앱 이름
                Text(
                    text = "NFC 복음",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A5F)
                )
            }
            // 검색 버튼
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "검색",
                    tint = Color(0xFF1E3A5F)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: (String) -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    val voiceViewModel: com.example.nfctapapp.ui.voice.VoiceCommandViewModel = hiltViewModel()
    val voiceUiState by voiceViewModel.uiState.collectAsState()

    // 권한 요청 런처
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition(context, voiceViewModel)
        } else {
            voiceViewModel.onRecognitionError("음성 인식 권한이 필요합니다")
        }
    }

    // ViewModel 초기화 (userId는 null 가능)
    LaunchedEffect(Unit) {
        voiceViewModel.initialize(null)
    }

    // 음성 명령 분석 완료 시 네비게이션
    LaunchedEffect(voiceUiState.analysis) {
        val analysis = voiceUiState.analysis
        if (analysis != null) {
            when (analysis.action) {
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.NAVIGATE_HOME -> {
                    // 이미 홈 화면
                }
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.NAVIGATE_SERMON -> onMenuClick("sermon")
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.NAVIGATE_CHAT -> onChatClick()
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.START_CHAT -> onChatClick()
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.SHOW_DAILY_VERSE -> onMenuClick("todayVerse")
                com.example.nfctapapp.data.remote.api.VoiceCommandAction.PLAY_LATEST_SERMON -> onMenuClick("sermon")
                else -> {
                    // UNKNOWN or unhandled
                }
            }
            // 분석 결과 소비 (한 번만 실행)
            voiceViewModel.consumeAnalysis()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .systemBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF1E3A5F)
            ) {
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Rounded.Book, contentDescription = "성경") },
                    label = { Text(text = "성경", fontSize = 11.sp) },
                    selected = false,
                    onClick = { onMenuClick("bible") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E3A5F),
                        selectedTextColor = Color(0xFF1E3A5F),
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Outlined.MusicNote, contentDescription = "찬양") },
                    label = { Text(text = "찬양", fontSize = 11.sp) },
                    selected = false,
                    onClick = { onMenuClick("worship") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E3A5F),
                        selectedTextColor = Color(0xFF1E3A5F),
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Rounded.Groups, contentDescription = "커뮤니티") },
                    label = { Text(text = "커뮤니티", fontSize = 11.sp) },
                    selected = false,
                    onClick = { onMenuClick("community") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E3A5F),
                        selectedTextColor = Color(0xFF1E3A5F),
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Outlined.EmojiEvents, contentDescription = "신앙활동") },
                    label = { Text(text = "신앙활동", fontSize = 11.sp) },
                    selected = false,
                    onClick = { onMenuClick("gamification") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E3A5F),
                        selectedTextColor = Color(0xFF1E3A5F),
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = Icons.Rounded.Campaign, contentDescription = "공동체") },
                    label = { Text(text = "공동체", fontSize = 11.sp) },
                    selected = false,
                    onClick = { onMenuClick("notice") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E3A5F),
                        selectedTextColor = Color(0xFF1E3A5F),
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 메인 타이틀
                Text(
                    text = "무엇을 도와드릴까요?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 설명
                Text(
                    text = "음성으로 말하거나 아래 버튼을 터치하세요",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 음성 인식 상태 표시
                when (voiceUiState.recognitionState) {
                    com.example.nfctapapp.ui.voice.VoiceRecognitionState.LISTENING -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = "🎤 음성을 듣고 있습니다...",
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    com.example.nfctapapp.ui.voice.VoiceRecognitionState.PROCESSING -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "🤖 명령을 분석하고 있습니다...",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                if (voiceUiState.recognizedText != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "\"${voiceUiState.recognizedText}\"",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    com.example.nfctapapp.ui.voice.VoiceRecognitionState.SUCCESS -> {
                        val message = voiceUiState.analysis?.message
                        if (message != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "✅ $message",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(20.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    com.example.nfctapapp.ui.voice.VoiceRecognitionState.ERROR -> {
                        if (voiceUiState.error != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE87B7B).copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "❌ ${voiceUiState.error}",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(20.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        // 기본 상태: 아무것도 표시하지 않음
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 큰 음성 인식 버튼
                FloatingActionButton(
                    onClick = {
                        when (voiceUiState.recognitionState) {
                            com.example.nfctapapp.ui.voice.VoiceRecognitionState.IDLE,
                            com.example.nfctapapp.ui.voice.VoiceRecognitionState.ERROR -> {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                            com.example.nfctapapp.ui.voice.VoiceRecognitionState.SUCCESS -> {
                                voiceViewModel.reset()
                            }
                            else -> {
                                // 진행 중일 때는 아무것도 안함
                            }
                        }
                    },
                    modifier = Modifier.size(100.dp),
                    containerColor = when (voiceUiState.recognitionState) {
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.LISTENING -> Color(0xFFE87B7B)
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.PROCESSING -> Color(0xFFF5A962)
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.SUCCESS -> Color(0xFF4CAF50)
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.ERROR -> Color(0xFFE87B7B).copy(alpha = 0.6f)
                        else -> Color.White
                    },
                    contentColor = when (voiceUiState.recognitionState) {
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.LISTENING,
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.PROCESSING,
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.SUCCESS,
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.ERROR -> Color.White
                        else -> Color(0xFF1E3A5F)
                    }
                ) {
                    when (voiceUiState.recognitionState) {
                        com.example.nfctapapp.ui.voice.VoiceRecognitionState.PROCESSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = Color.White
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "음성 인식",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 제안 명령어
                Text(
                    text = "이렇게 말해보세요:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "\"오늘 설교 들려줘\"",
                        "\"오늘의 말씀이 뭐야?\"",
                        "\"AI 상담 시작\"",
                        "\"성경 읽기\""
                    ).forEach { example ->
                        Text(
                            text = example,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // AI 채팅 플로팅 버튼 (우측 하단)
            FloatingActionButton(
                onClick = onChatClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1E3A5F)
            ) {
                Text(
                    text = "💬",
                    fontSize = 28.sp
                )
            }
        }
    }
}

private fun startVoiceRecognition(
    context: android.content.Context,
    viewModel: com.example.nfctapapp.ui.voice.VoiceCommandViewModel
) {
    val helper = com.example.nfctapapp.ui.voice.VoiceRecognitionHelper(
        context = context,
        onResult = { text -> viewModel.onRecognitionResult(text) },
        onError = { error -> viewModel.onRecognitionError(error) },
        onListeningStart = { viewModel.startListening() }
    )
    helper.startListening()
}

@Composable
fun TodayVerseScreen(onBackClick: () -> Unit) {
    val todayVerse = remember { VerseRepository.getTodayVerse() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.bg_home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 뒤로가기 버튼
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }

            // 오늘의 말씀 제목 (상단)
            Text(
                text = "오늘의 말씀",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                textAlign = TextAlign.Center
            )

            // 말씀 컨텐츠 (중앙 정렬)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = todayVerse.text,
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 3f
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = todayVerse.reference,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun HomeContentCard(
    title: String,
    onMoreClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A5F)
                )
                TextButton(onClick = onMoreClick) {
                    Text(
                        text = "더보기 →",
                        fontSize = 12.sp,
                        color = Color(0xFF6B8ED6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun HomeMenuCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E3A5F),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBackClick: () -> Unit,
    onMenuClick: (String) -> Unit
) {
    val menuItems = listOf(
        MenuItem("verse", "오늘의 말씀", "매일 새로운 말씀을 만나보세요", Icons.Rounded.Book, Color(0xFF6B8ED6)),
        MenuItem("sermon", "오늘의 설교", "은혜로운 설교를 들어보세요", Icons.Rounded.Campaign, Color(0xFFE87B7B)),
        MenuItem("notice", "교회 공지", "교회 소식을 확인하세요", Icons.Rounded.Notifications, Color(0xFFF5A962)),
        MenuItem("community", "커뮤니티", "성도들과 교제하세요", Icons.Rounded.Groups, Color(0xFF7BC47F)),
        MenuItem("settings", "설정", "앱 설정을 관리하세요", Icons.Rounded.Settings, Color(0xFF9E9E9E))
    )

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                Text(
                    text = "메뉴",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                menuItems.forEach { item ->
                    MenuCard(
                        item = item,
                        onClick = { onMenuClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuCard(
    item: MenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A5F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun SermonScreen(
    viewModel: SermonViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadSermonsIfNeeded()
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                Text(
                    text = "설교",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "오류가 발생했습니다",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E3A5F)
                                )
                            ) {
                                Text("다시 시도")
                            }
                        }
                    }
                }

                uiState.sermons.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 설교가 없습니다",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                else -> {
                    val latestSermon = uiState.latestSermon
                    val otherSermons = uiState.sermons.drop(1)

                    if (latestSermon != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE87B7B)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "최신 설교",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = latestSermon.title,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = buildString {
                                        append(latestSermon.preacher)
                                        if (latestSermon.scripture.isNotEmpty()) {
                                            append(" · ${latestSermon.scripture}")
                                        }
                                    },
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://www.youtube.com/watch?v=${latestSermon.youtubeVideoId}")
                                        )
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFFE87B7B)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "설교 듣기",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (otherSermons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "지난 설교",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(otherSermons) { sermon ->
                                SermonCard(
                                    sermon = sermon,
                                    onClick = {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://www.youtube.com/watch?v=${sermon.youtubeVideoId}")
                                        )
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SermonCard(
    sermon: com.example.nfctapapp.data.Sermon,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE87B7B).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFFE87B7B),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sermon.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A5F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sermon.preacher} · ${sermon.date}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = sermon.scripture,
                    fontSize = 13.sp,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}

data class MenuItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SplashScreen() {
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "로딩 중...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
