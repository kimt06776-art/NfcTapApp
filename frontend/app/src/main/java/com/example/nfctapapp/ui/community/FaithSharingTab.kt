package com.example.nfctapapp.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfctapapp.data.CommunityPost
import com.example.nfctapapp.data.CommunityRepository
import com.example.nfctapapp.data.PostType

@Composable
fun FaithSharingTab() {
    var selectedType by remember { mutableStateOf<PostType?>(null) }

    val posts = remember(selectedType) {
        if (selectedType == null) {
            CommunityRepository.getAllPosts()
        } else {
            CommunityRepository.getPostsByType(selectedType!!)
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
                    selected = selectedType == null,
                    onClick = { selectedType = null },
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

            items(PostType.entries.toList()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text("${type.emoji} ${type.displayName}", fontSize = 13.sp) },
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

        // 게시글 목록
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts) { post ->
                CommunityPostCard(post = post)
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CommunityPostCard(post: CommunityPost) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var likeCount by remember { mutableStateOf(post.likes) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // 작성자 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프로필 아이콘
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HiddenWarm.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.author.first().toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = post.author,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryText
                        )
                        Text(
                            text = "Lv.${post.authorLevel} · ${post.timestamp}",
                            fontSize = 11.sp,
                            color = TertiaryText
                        )
                    }
                }

                // 게시글 타입 배지
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HiddenWarm.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${post.type.emoji} ${post.type.displayName}",
                        fontSize = 11.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 게시글 내용
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = SecondaryText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = TertiaryText.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(8.dp))

            // 좋아요 & 댓글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 좋아요 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "좋아요",
                            tint = if (isLiked) HiddenWarm else TertiaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = likeCount.toString(),
                        fontSize = 13.sp,
                        color = TertiaryText
                    )
                }

                // 댓글
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "댓글",
                        tint = TertiaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.comments}개",
                        fontSize = 13.sp,
                        color = TertiaryText
                    )
                }
            }
        }
    }
}
