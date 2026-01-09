package com.example.nfctapapp.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nfctapapp.data.Notice
import com.example.nfctapapp.data.NoticeCategory
import com.example.nfctapapp.data.NoticeRepository

@Composable
fun ChurchNewsTab() {
    var selectedCategory by remember { mutableStateOf<NoticeCategory?>(null) }
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    val notices = remember(selectedCategory) {
        if (selectedCategory == null) {
            NoticeRepository.getAllNotices()
        } else {
            NoticeRepository.getNoticesByCategory(selectedCategory!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 카테고리 필터
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("전체", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HiddenWarm,
                        selectedLabelColor = PrimaryText,
                        containerColor = CardBackgroundDark,
                        labelColor = SecondaryText
                    ),
                    border = null
                )
            }

            items(NoticeCategory.entries.toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HiddenWarm,
                        selectedLabelColor = PrimaryText,
                        containerColor = CardBackgroundDark,
                        labelColor = SecondaryText
                    ),
                    border = null
                )
            }
        }

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

    // 공지 상세 다이얼로그
    selectedNotice?.let { notice ->
        NoticeDetailDialog(
            notice = notice,
            onDismiss = { selectedNotice = null }
        )
    }
}

@Composable
private fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit
) {
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
                // 카테고리 배지
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HiddenWarm.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = notice.category.displayName,
                        fontSize = 11.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 중요 표시
                if (notice.isImportant) {
                    Text(
                        text = "📌",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notice.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notice.content,
                fontSize = 13.sp,
                color = TertiaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notice.author,
                    fontSize = 11.sp,
                    color = TertiaryText.copy(alpha = 0.7f)
                )
                Text(
                    text = notice.date,
                    fontSize = 11.sp,
                    color = TertiaryText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NoticeDetailDialog(
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
                containerColor = DeepStone
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
                        shape = RoundedCornerShape(6.dp),
                        color = HiddenWarm.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = notice.category.displayName,
                            fontSize = 11.sp,
                            color = SecondaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = TertiaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제목
                Text(
                    text = notice.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 메타 정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notice.author,
                        fontSize = 12.sp,
                        color = TertiaryText
                    )
                    Text(
                        text = notice.date,
                        fontSize = 12.sp,
                        color = TertiaryText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = TertiaryText.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                // 본문
                Text(
                    text = notice.content,
                    fontSize = 14.sp,
                    color = SecondaryText,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 확인 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HiddenWarm
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "확인",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                }
            }
        }
    }
}
