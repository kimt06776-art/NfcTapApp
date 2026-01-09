package com.example.nfctapapp.ui.entrance

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 함께하심/임재 테마 말씀 (접속 시마다 랜덤)
 */
data class PresenceVerse(
    val text: String,
    val reference: String
)

object PresenceVerseRepository {
    private val verses = listOf(
        PresenceVerse(
            "두려워하지 말라\n내가 너와 함께 함이라",
            "이사야 41:10"
        ),
        PresenceVerse(
            "내가 세상 끝날까지\n너희와 항상 함께 있으리라",
            "마태복음 28:20"
        ),
        PresenceVerse(
            "여호와는 가까이 하는 모든 자\n곧 진실하게 가까이 하는\n모든 자에게 가까이 하시는도다",
            "시편 145:18"
        ),
        PresenceVerse(
            "내가 너를 떠나지 아니하고\n버리지 아니하리라",
            "여호수아 1:5"
        ),
        PresenceVerse(
            "볼지어다 내가 문 밖에 서서 두드리노니\n누구든지 내 음성을 듣고 문을 열면\n내가 그에게로 들어가\n그와 더불어 먹고\n그는 나와 더불어 먹으리라",
            "요한계시록 3:20"
        ),
        PresenceVerse(
            "네가 물 가운데로 지날 때에\n내가 너와 함께 할 것이라",
            "이사야 43:2"
        ),
        PresenceVerse(
            "여호와는 나의 목자시니\n내게 부족함이 없으리로다",
            "시편 23:1"
        ),
        PresenceVerse(
            "내가 너를 지명하여 불렀나니\n너는 내 것이라",
            "이사야 43:1"
        ),
        PresenceVerse(
            "하나님이 우리의 피난처시요 힘이시니\n환난 중에 만날 큰 도움이시라",
            "시편 46:1"
        ),
        PresenceVerse(
            "내 평안을 너희에게 주노라\n마음에 근심하지도 말고\n두려워하지도 말라",
            "요한복음 14:27"
        )
    )

    fun getRandomVerse(): PresenceVerse {
        return verses.random()
    }
}

// 앱 컬러 시스템 (colors.md 참고)
private object AppColors {
    // 배경
    val StoneGray = Color(0xFF7B7A77)        // 배경 상단
    val DeepStone = Color(0xFF4F4E4B)        // 배경 하단/깊이

    // 텍스트
    val PrimaryText = Color(0xFFF5F4F2)      // 주 텍스트 (순백색 대신)
    val SecondaryText = Color(0xFFD8D6D2)    // 보조 텍스트
    val TertiaryText = Color(0xFFC1BFBB)     // UI 텍스트
}

/**
 * Entrance Screen - 하나님의 임재를 느끼는 입구
 * 5초 후 자동으로 다음 화면으로 전환
 */
@Composable
fun EntranceScreen(
    onTimeout: () -> Unit
) {
    val verse = remember { PresenceVerseRepository.getRandomVerse() }

    // 5초 후 자동 전환
    LaunchedEffect(Unit) {
        delay(5000)
        onTimeout()
    }

    // 배경 밝기 애니메이션 (3초 주기, 2~3% 변화)
    val infiniteTransition = rememberInfiniteTransition(label = "brightness")
    val brightnessAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brightnessAlpha"
    )

    // 밝기 애니메이션이 적용된 배경색 계산
    val animatedStoneGray = AppColors.StoneGray.copy(
        red = AppColors.StoneGray.red * (1f - brightnessAlpha * 0.1f),
        green = AppColors.StoneGray.green * (1f - brightnessAlpha * 0.1f),
        blue = AppColors.StoneGray.blue * (1f - brightnessAlpha * 0.1f)
    )
    val animatedDeepStone = AppColors.DeepStone.copy(
        red = AppColors.DeepStone.red * (1f - brightnessAlpha * 0.1f),
        green = AppColors.DeepStone.green * (1f - brightnessAlpha * 0.1f),
        blue = AppColors.DeepStone.blue * (1f - brightnessAlpha * 0.1f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedStoneGray, animatedDeepStone)
                )
            )
    ) {

        // 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 말씀 텍스트 (Primary Text 색상)
            Text(
                text = verse.text,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.PrimaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = 5f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 말씀 출처 (Secondary Text 색상)
            Text(
                text = verse.reference,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.SecondaryText,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // 함께하심 메시지 (Secondary Text 색상)
            Text(
                text = "지금 이 순간에도\n하나님은 당신과 함께 계십니다",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light,
                    color = AppColors.SecondaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
