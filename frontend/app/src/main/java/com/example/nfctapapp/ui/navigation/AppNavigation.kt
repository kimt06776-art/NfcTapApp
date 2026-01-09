package com.example.nfctapapp.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nfctapapp.domain.model.AuthState
import com.example.nfctapapp.ui.auth.AuthViewModel
import com.example.nfctapapp.ui.auth.RegistrationScreen
import com.example.nfctapapp.ui.auth.WelcomeScreen
import com.example.nfctapapp.ui.chat.ChatScreen
import com.example.nfctapapp.ui.entrance.EntranceScreen
import com.example.nfctapapp.ui.home.HomeScreen
import com.example.nfctapapp.ui.sermon.SermonScreen
import com.example.nfctapapp.ui.sermon.SermonViewModel
import com.example.nfctapapp.ui.splash.SplashScreen
import com.example.nfctapapp.ui.verse.TodayVerseScreen

@Composable
fun AppNavigation(
    nfcTagUid: String?,
    initialNfcUid: String?,
    onNfcConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

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

    // 인증 상태에 따른 화면 전환
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("entrance") {
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

        composable("entrance") {
            EntranceScreen(
                onTimeout = {
                    navController.navigate("home") {
                        popUpTo("entrance") { inclusive = true }
                    }
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
            com.example.nfctapapp.ui.bible.BibleBookSelectorScreen(
                currentBook = "요한복음",
                currentChapter = 1,
                onBookSelected = { bookName, chapter, verse ->
                    if (bookName == "요한복음") {
                        navController.navigate("bible?chapter=$chapter&verse=$verse") {
                            popUpTo("bibleBookSelector") { inclusive = true }
                        }
                    } else {
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

        composable("community") {
            com.example.nfctapapp.ui.community.CommunityScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("sermonNote") {
            com.example.nfctapapp.ui.sermonnote.SermonNoteScreen(
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
