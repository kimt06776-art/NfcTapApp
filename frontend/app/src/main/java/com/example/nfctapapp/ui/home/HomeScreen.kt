package com.example.nfctapapp.ui.home

import java.util.Calendar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctapapp.R

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
    onMenuClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    userName: String = "사용자"
) {
    var showMenuSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // 햄버거 메뉴 바텀시트
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = sheetState,
            containerColor = DeepStone,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(TertiaryText.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "메뉴",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                HorizontalDivider(color = TertiaryText.copy(alpha = 0.2f))

                // 설교 노트 메뉴
                MenuSheetItem(
                    icon = Icons.Outlined.Description,
                    title = "설교 노트",
                    subtitle = "예배 중 설교 내용을 기록하세요",
                    onClick = {
                        showMenuSheet = false
                        onMenuClick("sermonNote")
                    }
                )

                // 설정 메뉴 (추후 추가용)
                MenuSheetItem(
                    icon = Icons.Outlined.Settings,
                    title = "설정",
                    subtitle = "앱 설정 및 계정 관리",
                    onClick = {
                        showMenuSheet = false
                        // TODO: 설정 화면으로 이동
                    }
                )
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            HomeBottomBar(
                onBibleClick = { onMenuClick("bible") },
                onCommunityClick = { onMenuClick("community") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7B7A77),  // Stone Gray (top)
                            Color(0xFF4F4E4B)   // Deep Stone (bottom)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 히어로 이미지 + 오버레이 아이콘
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_hero),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(0f, -0.5f)  // 수직 -0.5 (Top과 Center 사이)
                    )

                    // 상단 아이콘 (알림, 햄버거 메뉴)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNotificationClick) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "알림",
                                tint = Color(0xFFF5F4F2),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { showMenuSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "메뉴",
                                tint = Color(0xFFF5F4F2),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 시간대별 인사말
                    val greeting = remember {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        when {
                            hour in 5..11 -> "좋은 아침이에요!"
                            hour in 12..17 -> "좋은 오후예요!"
                            else -> "좋은 저녁이에요!"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // 프로필 이미지
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFD8D6D2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤",
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "$userName 님,",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF5F4F2)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = greeting,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F4F2)
                        )
                    }
                }

                // 최근 선택 섹션
                Text(
                    text = "최근에 선택했어요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD8D6D2),  // Secondary Text
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(vertical = 30.dp)
                )

                // 최근 선택 콘텐츠 카드 (가로 스크롤)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RecentContentCard(
                            imageRes = R.drawable.today_word,
                            title = "오늘의 말씀",
                            subtitle = "3분 전",
                            modifier = Modifier.width(150.dp),
                            onClick = { }
                        )
                    }
                    item {
                        RecentContentCard(
                            imageRes = R.drawable.sunday,
                            title = "주일 설교",
                            subtitle = "2일 전",
                            modifier = Modifier.width(150.dp),
                            onClick = { }
                        )
                    }
                    item {
                        RecentContentCard(
                            imageRes = R.drawable.today_song,
                            title = "오늘의 찬양",
                            subtitle = "5일 전",
                            modifier = Modifier.width(150.dp),
                            onClick = { }
                        )
                    }
                    item {
                        RecentContentCard(
                            imageRes = R.drawable.today_pray,
                            title = "오늘의 기도",
                            subtitle = "1주 전",
                            modifier = Modifier.width(150.dp),
                            onClick = { }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI 대화 박스
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(onClick = onChatClick),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF9A8F7A)  // Hidden Warm (accent)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "누아와 대화하기",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF5F4F2)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "무엇이든 물어보세요",
                                fontSize = 13.sp,
                                color = Color(0xFFE8E6E2)
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 20.sp,
                            color = Color(0xFFF5F4F2)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onBibleClick: () -> Unit,
    onCommunityClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4F4E4B))  // Deep Stone
    ) {
        // 구분선
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.5.dp,
            color = Color(0xFF7B7A77)  // Stone Gray
        )

        // 버튼들
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 성경 버튼
            Column(
                modifier = Modifier.clickable(onClick = onBibleClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Book,
                    contentDescription = "성경",
                    tint = Color(0xFFC1BFBB),  // Tertiary Text
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "성경",
                    fontSize = 11.sp,
                    color = Color(0xFFC1BFBB)  // Tertiary Text
                )
            }

            // 공동체 버튼
            Column(
                modifier = Modifier.clickable(onClick = onCommunityClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = "공동체",
                    tint = Color(0xFFC1BFBB),  // Tertiary Text
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "공동체",
                    fontSize = 11.sp,
                    color = Color(0xFFC1BFBB)  // Tertiary Text
                )
            }
        }
    }
}

@Composable
private fun RecentContentCard(
    imageRes: Int,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8E8D8A)
        )
    ) {
        Column {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF5F4F2)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFFC1BFBB)  // Tertiary Text
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = HiddenWarm,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TertiaryText
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TertiaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}
