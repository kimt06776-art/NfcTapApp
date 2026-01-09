package com.example.nfctapapp.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// colors.md 색상 시스템
internal val StoneGray = Color(0xFF7B7A77)
internal val DeepStone = Color(0xFF4F4E4B)
internal val HiddenWarm = Color(0xFF9A8F7A)
internal val PrimaryText = Color(0xFFF5F4F2)
internal val SecondaryText = Color(0xFFD8D6D2)
internal val TertiaryText = Color(0xFFC1BFBB)
internal val CardBackgroundDark = Color(0xFF5A5957)

@Composable
fun CommunityScreen(onBackClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("신앙 나눔", "교회 소식")

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = PrimaryText
                    )
                }

                Text(
                    text = "공동체",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            // 탭 바
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryText,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 2.dp,
                        color = HiddenWarm
                    )
                },
                divider = {
                    HorizontalDivider(
                        color = TertiaryText.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryText else SecondaryText
                            )
                        }
                    )
                }
            }

            // 탭 컨텐츠
            when (selectedTab) {
                0 -> FaithSharingTab()
                1 -> ChurchNewsTab()
            }
        }
    }
}
