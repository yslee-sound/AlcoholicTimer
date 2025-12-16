package kr.sweetapps.alcoholictimer.ui.tab_04.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phase 2: 커뮤니티 피드 메인 화면 (Firestore 연동)
 * 페이스북 스타일의 수직 스크롤 피드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5), // 연한 회색 배경
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.community_title), // "익명 응원 챌린지"
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF111111)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && posts.isEmpty()) {
                // 로딩 중
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (posts.isEmpty()) {
                // 게시글 없음
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                // 게시글 목록
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // [NEW Phase 3] 6번째 아이템마다 광고 삽입
                    val itemsWithAds = posts.flatMapIndexed { index, post ->
                        if ((index + 1) % 6 == 0 && index > 0) {
                            listOf(post, null) // null은 광고 슬롯
                        } else {
                            listOf(post)
                        }
                    }

                    items(itemsWithAds.size, key = { index ->
                        val item = itemsWithAds[index]
                        item?.id ?: "ad_$index"
                    }) { index ->
                        val item = itemsWithAds[index]

                        if (item == null) {
                            // [NEW Phase 3] 네이티브 광고 슬롯
                            NativeAdItem()
                        } else {
                            PostItem(
                                nickname = item.nickname,
                                timerDuration = item.timerDuration,
                                content = item.content,
                                imageUrl = item.imageUrl,
                                likeCount = item.likeCount,
                                isLiked = false, // Phase 3에서 사용자별 좋아요 상태 관리
                                remainingTime = calculateRemainingTime(item.deleteAt),
                                onLikeClick = {
                                    viewModel.toggleLike(item.id)
                                },
                                onCommentClick = {
                                    // Phase 3: 댓글 기능
                                },
                                onMoreClick = {
                                    // Phase 3: 더보기 메뉴
                                }
                            )
                        }

                        // 게시글 사이 구분선
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 빈 상태 표시
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📝",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아직 게시글이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF666666)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tab 5 디버그 메뉴에서\n테스트 게시글을 생성해 보세요!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF999999),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * 남은 시간 계산 (deleteAt - now)
 */
private fun calculateRemainingTime(deleteAt: Timestamp): String {
    val now = System.currentTimeMillis()
    val deleteAtMillis = deleteAt.seconds * 1000
    val diffMillis = deleteAtMillis - now

    if (diffMillis <= 0) return "만료됨"

    val hours = (diffMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((diffMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

    return when {
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "곧 만료"
    }
}

/**
 * [NEW Phase 3] 네이티브 광고 아이템
 * PostItem과 동일한 디자인으로 통일
 */
@Composable
private fun NativeAdItem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFBF0)) // 연한 노란색 배경으로 광고임을 표시
            .padding(16.dp)
    ) {
        // "광고" 라벨
        Text(
            text = "Sponsored",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF999999),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Phase 3: 실제 네이티브 광고 컴포넌트는 추후 구현
        // 현재는 플레이스홀더 표시
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📢 Native Ad Placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

// ===== Preview (Phase 1 호환) =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CommunityScreenPreview() {
    MaterialTheme {
        // Preview용 더미 데이터는 Phase 1 코드 유지
        CommunityScreenWithDummyData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityScreenWithDummyData() {
    val dummyPosts = remember {
        listOf(
            DummyPost(
                id = "1",
                nickname = "익명의 사자",
                timerDuration = "72시간",
                content = "오늘도 술 없이 하루를 보냈습니다. 처음엔 힘들었지만 점점 익숙해지고 있어요.",
                imageUrl = "https://picsum.photos/seed/1/400/300",
                likeCount = 24,
                isLiked = false,
                remainingTime = "5h"
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = { Text(text = "익명 응원 챌린지") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(dummyPosts) { post ->
                PostItem(
                    nickname = post.nickname,
                    timerDuration = post.timerDuration,
                    content = post.content,
                    imageUrl = post.imageUrl,
                    likeCount = post.likeCount,
                    isLiked = post.isLiked,
                    remainingTime = post.remainingTime
                )
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            }
        }
    }
}

/**
 * Phase 1 호환 더미 데이터 모델
 */
data class DummyPost(
    val id: String,
    val nickname: String,
    val timerDuration: String,
    val content: String,
    val imageUrl: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val remainingTime: String
)
