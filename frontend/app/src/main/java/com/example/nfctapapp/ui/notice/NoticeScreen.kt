package com.example.nfctapapp.ui.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nfctapapp.data.Notice
import com.example.nfctapapp.data.NoticeCategory
import com.example.nfctapapp.data.NoticeRepository

@Composable
fun NoticeScreen(onBackClick: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<NoticeCategory?>(null) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    val notices = remember {
        if (selectedCategory == null) {
            NoticeRepository.getAllNotices()
        } else {
            NoticeRepository.getNoticesByCategory(selectedCategory!!)
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
                        tint = Color.White
                    )
                }

                Text(
                    text = "교회 공지",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 카테고리 필터
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("전체") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color(0xFF1E3A5F),
                        containerColor = Color.White.copy(alpha = 0.3f),
                        labelColor = Color.White
                    )
                )

                NoticeCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(category.color),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.3f),
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 공지 목록
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notices) { notice ->
                    NoticeCard(
                        notice = notice,
                        onClick = { selectedNotice = notice }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // 공지 상세 다이얼로그
    selectedNotice?.let { notice ->
        NoticeDetailDialog(
            notice = notice,
            onDismiss = { selectedNotice = null }
        )
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
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
                // 카테고리 배지
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(notice.category.color)
                ) {
                    Text(
                        text = notice.category.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 중요 표시
                if (notice.isImportant) {
                    Text(
                        text = "📌",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notice.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A5F)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notice.content.take(50) + if (notice.content.length > 50) "..." else "",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notice.author,
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Text(
                    text = notice.date,
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}

@Composable
fun NoticeDetailDialog(
    notice: Notice,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(notice.category.color)
                    ) {
                        Text(
                            text = notice.category.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 제목
                Text(
                    text = notice.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A5F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 메타 정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notice.author,
                        fontSize = 13.sp,
                        color = Color(0xFF888888)
                    )
                    Text(
                        text = notice.date,
                        fontSize = 13.sp,
                        color = Color(0xFF888888)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color(0xFFE0E0E0))

                Spacer(modifier = Modifier.height(16.dp))

                // 본문
                Text(
                    text = notice.content,
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 확인 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(notice.category.color)
                    )
                ) {
                    Text(
                        text = "확인",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
