package kr.sweetapps.alcoholictimer.ui.tab_03.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kr.sweetapps.alcoholictimer.util.AvatarManager
import kr.sweetapps.alcoholictimer.ui.theme.MainPrimaryBlue

/**
 * Phase 1: 커뮤니티 게시글 아이템 UI
 * 페이스북 스타일의 Full-width 디자인
 * (v2.0) 아바타 시스템: authorAvatarIndex로 프로필 표시
 * (v3.0) X 버튼: 남의 글에 빠른 숨기기 버튼 추가
 */
@Composable
fun PostItem(
    nickname: String,
    timerDuration: String, // "72시간" 형식 (하위호환성)
    content: String,
    imageUrl: String? = null,
    likeCount: Int,
    isLiked: Boolean = false,
    remainingTime: String, // "5h" 형식 (하위호환성)
    currentDays: Int = 1,
    userLevel: Int = 1,
    authorAvatarIndex: Int = 0, // [NEW] 아바타 인덱스
    thirstLevel: Int? = null, // [NEW] 갈증 수치 표시
    isMine: Boolean = false, // [NEW] Phase 3: 내 글 여부
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHideClick: () -> Unit = {} // [NEW] Phase 3: 숨기기 (X 버튼)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Header: 프로필 + 닉네임 + 타이머 배지 + X 버튼 + 더보기
        PostHeader(
            nickname = nickname,
            timerDuration = timerDuration,
            currentDays = currentDays,
            userLevel = userLevel,
            authorAvatarIndex = authorAvatarIndex, // [NEW]
            thirstLevel = thirstLevel, // [NEW]
            isMine = isMine, // [NEW] Phase 3
            onMoreClick = onMoreClick,
            onHideClick = onHideClick // [NEW] Phase 3
        )

        // Body: 텍스트 본문 (최대 5줄, 클릭 시 펼치기/접기)
        if (content.isNotBlank()) {
            var isExpanded by remember { mutableStateOf(false) }

            val interactionSource = remember { MutableInteractionSource() }

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1F2937),
                maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isExpanded = !isExpanded }
            )
        }

        // Body: 이미지 (선택사항) - [FIX] AsyncImage로 실제 이미지 표시 (2025-12-19)
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "게시글 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // 높이 제한 없이 원본 비율대로
                    .clip(RoundedCornerShape(0.dp)), // 모서리 각지게 (페이스북 스타일)
                contentScale = ContentScale.FillWidth // 가로를 꽉 채우고 세로는 비율 유지
            )
        }

        // Footer: 좋아요 + 댓글 + 남은 시간
        PostFooter(
            likeCount = likeCount,
            isLiked = isLiked,
            remainingTime = remainingTime,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick
        )
    }
}

/**
 * 게시글 헤더: 프로필 아이콘 + 닉네임 + 타이머 배지 + 더보기 메뉴
 * (v2.0) 아바타 이미지 표시
 */
/**
 * 게시글 헤더: 프로필 + 닉네임 + 타이머 배지 + X 버튼 + 더보기
 * (v3.0) X 버튼: 남의 글에만 표시 (빠른 숨기기)
 */
@Composable
private fun PostHeader(
    nickname: String,
    timerDuration: String,
    currentDays: Int = 1,
    userLevel: Int = 1,
    authorAvatarIndex: Int = 0, // [NEW]
    thirstLevel: Int? = null, // [NEW] 갈증 수치
    isMine: Boolean = false, // [NEW] Phase 3
    onMoreClick: () -> Unit,
    onHideClick: () -> Unit = {} // [NEW] Phase 3
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top // [FIX] 버튼을 별명과 같은 줄에 정렬 (2025-12-20)
    ) {
        // [NEW] 아바타 이미지 (로컬 리소스)
        Image(
            painter = painterResource(id = AvatarManager.getAvatarResId(authorAvatarIndex)),
            contentDescription = "프로필",
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color(0xFFE0E0E0), CircleShape) // 회색 테두리
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 닉네임 + LV/일차: 닉네임 너비 기준으로 왼쪽 정렬
        Column(
            modifier = Modifier.wrapContentWidth(),
            horizontalAlignment = Alignment.Start // [FIX] 중앙 정렬 -> 왼쪽 정렬로 변경
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111111)
                )

                // [NEW] 갈증 수치가 있으면 닉네임 옆에 구분자, 숫자 뱃지, 후행 텍스트를 표시합니다.
                if (thirstLevel != null) {
                    Spacer(modifier = Modifier.width(4.dp))

                    // 구분자
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // 색상 매핑 함수 (WritePostScreenContent와 동일한 규칙)
                    val badgeColor = when (thirstLevel) {
                        in 1..3 -> Color(0xFF4CAF50)
                        in 4..7 -> Color(0xFFFFA726)
                        else -> Color(0xFFE53935)
                    }

                    // 숫자 뱃지 (Rounded box)
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thirstLevel.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 후행 텍스트
                    Text(
                        text = " 갈증",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 레벨 및 일차 정보 Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LV.$userLevel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MainPrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline() // [추가] 글자 밑줄 기준 정렬
                )

                Spacer(modifier = Modifier.width(4.dp)) // [FIX] 구분자 간격 좁힘

                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.alignByBaseline()
                )

                Spacer(modifier = Modifier.width(4.dp)) // [FIX] 구분자 간격 좁힘

                Text(
                    text = "${currentDays}일차",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.alignByBaseline() // [추가] 글자 밑줄 기준 정렬
                )
            }
        }

        // 남은 공간을 차지하여 오른쪽 아이콘들이 끝으로 밀리도록 함
        Spacer(modifier = Modifier.weight(1f))

        // [MODIFIED] 버튼 순서: 3점 버튼 → X 버튼 (페이스북 스타일) (2025-12-20)
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .size(40.dp)
                .offset(y = (-4).dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "더보기",
                tint = Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
        }

        if (!isMine) {
            IconButton(
                onClick = onHideClick,
                modifier = Modifier
                    .size(40.dp)
                    .offset(y = (-4).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "숨기기",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 게시글 푸터: 좋아요(쓰담쓰담) + 댓글 + 남은 시간
 */
@Composable
private fun PostFooter(
    likeCount: Int,
    isLiked: Boolean,
    remainingTime: String,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), // [FIX] 하단 패딩 줄임 (2025-12-20)
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좋아요(쓰담쓰담) 버튼 + 카운트
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onLikeClick() }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "쓰담쓰담",
                tint = if (isLiked) Color(0xFFE91E63) else Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = likeCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // [FIX] 댓글 기능은 다음 버전(MVP 이후)으로 연기 -> UI 숨김 처리 (2025-12-20)
        /*
        // 댓글 버튼
        IconButton(onClick = onCommentClick) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "댓글",
                tint = Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
        }
        */

        Spacer(modifier = Modifier.weight(1f))

        // 남은 시간 (우측 끝)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⏳",
                fontSize = 14.sp
            )
            Text(
                text = remainingTime,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999)
            )
        }
    }
}

// ===== Preview =====

@Preview(showBackground = true)
@Composable
fun PostItemPreview() {
    PostItem(
        nickname = "익명의 사자",
        timerDuration = "72시간",
        content = "오늘도 술 없이 하루를 보냈습니다. 처음엔 힘들었지만 점점 익숙해지고 있어요. 여러분도 할 수 있습니다!",
        imageUrl = "https://picsum.photos/400/300",
        likeCount = 24,
        isLiked = false,
        remainingTime = "5h"
    )
}

@Preview(showBackground = true)
@Composable
fun PostItemWithoutImagePreview() {
    PostItem(
        nickname = "참는 중인 호랑이",
        timerDuration = "48시간",
        content = "3일차인데 생각보다 괜찮네요. 아침에 일어나는 게 훨씬 가벼워요 😊",
        imageUrl = null,
        likeCount = 12,
        isLiked = true,
        remainingTime = "18h"
    )
}
