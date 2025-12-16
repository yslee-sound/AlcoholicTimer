package kr.sweetapps.alcoholictimer.ui.tab_04.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R

/**
 * Phase 1: 커뮤니티 피드 메인 화면
 * 페이스북 스타일의 수직 스크롤 피드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen() {
    // Phase 1: 하드코딩된 더미 데이터
    val dummyPosts = remember {
        listOf(
            DummyPost(
                id = "1",
                nickname = "익명의 사자",
                timerDuration = "72시간",
                content = "오늘도 술 없이 하루를 보냈습니다. 처음엔 힘들었지만 점점 익숙해지고 있어요. 여러분도 할 수 있습니다!",
                imageUrl = "https://picsum.photos/seed/1/400/300",
                likeCount = 24,
                isLiked = false,
                remainingTime = "5h"
            ),
            DummyPost(
                id = "2",
                nickname = "참는 중인 호랑이",
                timerDuration = "48시간",
                content = "3일차인데 생각보다 괜찮네요. 아침에 일어나는 게 훨씬 가벼워요 😊",
                imageUrl = null,
                likeCount = 12,
                isLiked = false,
                remainingTime = "18h"
            ),
            DummyPost(
                id = "3",
                nickname = "익명 1",
                timerDuration = "120시간",
                content = "5일 달성! 친구들이 술 마시자고 할 때가 제일 힘들지만 거절하는 연습을 하고 있어요.",
                imageUrl = "https://picsum.photos/seed/3/400/300",
                likeCount = 45,
                isLiked = true,
                remainingTime = "12h"
            ),
            DummyPost(
                id = "4",
                nickname = "새벽의 독수리",
                timerDuration = "96시간",
                content = "술 없이 보낸 주말이 이렇게 길게 느껴질 줄은 몰랐어요. 그래도 뿌듯합니다!",
                imageUrl = null,
                likeCount = 8,
                isLiked = false,
                remainingTime = "22h"
            ),
            DummyPost(
                id = "5",
                nickname = "밤하늘의 별",
                timerDuration = "168시간",
                content = "일주일을 채웠습니다! 🎉 건강검진 결과가 좋아졌어요. 계속 이어갈게요!",
                imageUrl = "https://picsum.photos/seed/5/400/300",
                likeCount = 67,
                isLiked = false,
                remainingTime = "2h"
            ),
            DummyPost(
                id = "6",
                nickname = "조용한 늑대",
                timerDuration = "24시간",
                content = "하루만 해보자는 마음으로 시작했는데 여기까지 왔네요. 작은 성공이 큰 힘이 됩니다.",
                imageUrl = null,
                likeCount = 15,
                isLiked = true,
                remainingTime = "8h"
            ),
            DummyPost(
                id = "7",
                nickname = "아침의 햇살",
                timerDuration = "200시간",
                content = "8일째! 숙면을 취하니까 피부도 좋아지고 기분도 상쾌해요. 앞으로도 화이팅!",
                imageUrl = "https://picsum.photos/seed/7/400/300",
                likeCount = 33,
                isLiked = false,
                remainingTime = "15h"
            )
        )
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp) // 간격 없음 (구분선만 사용)
        ) {
            items(dummyPosts) { post ->
                PostItem(
                    nickname = post.nickname,
                    timerDuration = post.timerDuration,
                    content = post.content,
                    imageUrl = post.imageUrl,
                    likeCount = post.likeCount,
                    isLiked = post.isLiked,
                    remainingTime = post.remainingTime,
                    onLikeClick = {
                        // Phase 1: 아직 기능 없음
                    },
                    onCommentClick = {
                        // Phase 1: 아직 기능 없음
                    },
                    onMoreClick = {
                        // Phase 1: 아직 기능 없음
                    }
                )

                // 게시글 사이 구분선 (1dp, 얇은 회색)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}

/**
 * Phase 1: 더미 데이터 모델
 * Phase 2에서 Firestore 모델로 교체 예정
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

// ===== Preview =====

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CommunityScreenPreview() {
    MaterialTheme {
        CommunityScreen()
    }
}

