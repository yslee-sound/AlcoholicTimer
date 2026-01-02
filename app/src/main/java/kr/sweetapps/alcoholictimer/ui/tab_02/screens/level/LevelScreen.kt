package kr.sweetapps.alcoholictimer.ui.tab_02.screens.level

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel
import kr.sweetapps.alcoholictimer.ui.common.LevelCard
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelListCard
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.util.manager.UserStatusManager // [NEW] 중앙 관리자 추가 (2025-12-25)

/**
 * Tab03 - 레벨 화면
 * 사용자의 금주 레벨 진행 상황을 보여주는 메인 화면
 * ViewModel을 Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
 *
 * [UPDATED] UserStatusManager 완전 통합 (2025-12-25)
 * - 누적(Total) 일수/레벨 기준으로 표시
 * - totalDaysPrecise(Float) 사용으로 부드러운 프로그레스 진행
 * - Single Source of Truth로 완벽한 동기화 보장
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: Tab03ViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // [UPDATED] UserStatusManager에서 정밀한 데이터 가져오기 (2025-12-25)
    val userStatus by UserStatusManager.userStatus.collectAsState()
    val currentDays = userStatus.days
    val currentLevel = LevelDefinitions.getLevelInfo(currentDays)
    val totalDaysPrecise = userStatus.totalDaysPrecise // ★ 핵심: Float 정밀값

    // ViewModel에서 상태 구독 (광고 정책용만)
    val startTime by viewModel.startTime.collectAsState()
    val isTimerCompleted by viewModel.isTimerCompleted.collectAsState() // [NEW] 타이머 완료 여부 구독 (2026-01-02)
    val levelVisits by viewModel.levelVisits.collectAsState()

    // ...existing code (BackHandler)...

    // 뒤로가기 처리: 광고 정책 확인 (수익화 핵심 로직)
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = true) {
        Log.d("LevelBack", "BackHandler triggered: policy check start")
        coroutineScope.launch {
            try {
                val act = activity
                Log.d("LevelBack", "Back pressed, level visits=$levelVisits")

                if (act == null || levelVisits < 3) {
                    try { onNavigateBack() } catch (_: Throwable) {}
                } else {
                    try {
                        Log.d("LevelBack", "AdController.snapshot-before -> ${AdController.debugSnapshot()}")
                        val allowed = AdController.canShowInterstitial(context)
                        Log.d("LevelBack", "AdController.canShowInterstitial returned=$allowed | snapshot-after -> ${AdController.debugSnapshot()}")

                        if (!allowed) {
                            Log.d("LevelBack", "Interstitial suppressed by AdController policy")
                            try { onNavigateBack() } catch (_: Throwable) {}
                        } else {
                            val showed = InterstitialAdManager.maybeShowIfEligible(act) {
                                Log.d("LevelBack", "Interstitial dismissed callback -> ${AdController.debugSnapshot()}")
                                viewModel.resetLevelVisits()
                                try { onNavigateBack() } catch (_: Throwable) {}
                            }
                            Log.d("LevelBack", "maybeShowIfEligible returned: $showed")
                            if (!showed) try { onNavigateBack() } catch (_: Throwable) {}
                        }
                    } catch (t: Throwable) {
                        Log.e("LevelBack", "ad check failed, navigating back", t)
                        try { onNavigateBack() } catch (_: Throwable) {}
                    }
                }
            } catch (t: Throwable) {
                Log.e("LevelBack", "BackHandler coroutine failed", t)
                activity?.finish()
            }
        }
    }

    // [NEW] Scaffold with BackTopBar + Edge-to-Edge 이슈 해결
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White, // [FIX] 배경색 명시적 지정
        contentWindowInsets = WindowInsets.systemBars, // [FIX] 시스템 바 영역 확보
        topBar = {
            BackTopBar(
                title = stringResource(R.string.level_screen_title),
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        // [UPDATED] UserStatusManager의 정밀값 전달 (2025-12-25)
        LevelScreenContent(
            innerPadding = innerPadding,
            currentLevel = currentLevel,
            levelDays = currentDays,
            totalDaysPrecise = totalDaysPrecise, // ★ Float 정밀값 전달
            startTime = startTime,
            isTimerCompleted = isTimerCompleted, // [NEW] 타이머 완료 여부 전달 (2026-01-02)
            viewModel = viewModel
        )
    }
}

// [NEW] LevelScreen Content 분리
@Composable
fun LevelScreenContent(
    innerPadding: PaddingValues,
    currentLevel: LevelDefinitions.LevelInfo,
    levelDays: Int,
    totalDaysPrecise: Float, // [CHANGED] totalElapsedDaysFloat → totalDaysPrecise
    startTime: Long?,
    isTimerCompleted: Boolean, // [NEW] 타이머 완료 여부 (인디케이터 색상 제어) (2026-01-02)
    viewModel: Tab03ViewModel
) {
    // [FIX] innerPadding을 제대로 적용하고 배경색을 White로 고정
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // [FIX] MaterialTheme.colorScheme.surface → Color.White (색상 변질 방지)
            .padding(innerPadding) // [FIX] innerPadding 적용 (시스템 바 영역 확보)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), // [FIX] 수평 패딩만 별도 적용
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // [REMOVED] 상단 Spacer 제거 - innerPadding이 이미 처리함

        Spacer(modifier = Modifier.height(7.dp)) // [UPDATED] BackTopBar(56dp) vs ModernDashboardHeader(48dp) 8dp 차이 고려 (15dp - 8dp = 7dp)

        // [MODIFIED] 공통 LevelCard 컴포넌트 사용 (2025-12-23)
        // [FIX] 정확한 구간 진행률 계산 (2025-12-25)
        // [UPDATED] UserStatusManager의 totalDaysPrecise 사용으로 완벽한 동기화 (2025-12-25)
        LevelCard(
            currentLevel = currentLevel,
            currentDays = levelDays,
            progress = LevelDefinitions.getLevelProgress(totalDaysPrecise), // ★ totalDaysPrecise 사용
            startTime = startTime ?: 0L, // [NEW] 인디케이터 표시용 (2026-01-02)
            isTimerCompleted = isTimerCompleted, // [NEW] 인디케이터 색상 제어용 (2026-01-02)
            containerColor = Color(0xFF1E40AF), // Deep Blue
            cardHeight = 200.dp,
            showDetailedInfo = true,
            onClick = null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp)) // 섹션 간 간격

        // 전체 레벨 리스트
        LevelListCard(
            currentLevel = currentLevel,
            currentDays = levelDays
        )

        Spacer(modifier = Modifier.height(20.dp)) // [NEW] 하단 여백 (스크롤 여유)
    }
}

// Compose Preview
@Preview(
    name = "Tab03 - 레벨 화면 전체",
    showBackground = true,
    heightDp = 800,
    showSystemUi = true
)
@Composable
private fun PreviewLevelScreen() {
    MaterialTheme {
        LevelScreen(
            onNavigateBack = {}
        )
    }
}

