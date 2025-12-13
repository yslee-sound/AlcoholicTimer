package kr.sweetapps.alcoholictimer.ui.tab_03.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.components.CurrentLevelCard
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelListCard
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelDefinitions

/**
 * Tab03 - 레벨 화면
 * 사용자의 금주 레벨 진행 상황을 보여주는 메인 화면
 * ViewModel을 Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: Tab03ViewModel = viewModel(
        viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity
    )
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // ViewModel에서 상태 구독
    val startTime by viewModel.startTime.collectAsState()
    val levelVisits by viewModel.levelVisits.collectAsState()
    val totalElapsedDaysFloat by viewModel.totalElapsedDaysFloat.collectAsState()
    val levelDays by viewModel.levelDays.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()

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

    // [NEW] Scaffold with TopAppBar - HabitScreen과 동일한 구조
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.level_title), // "레벨 확인"
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111)
                    )
                },
                // [FIX] navigationIcon 슬롯 제거 - 뒤로 가기 아이콘 없음
                // [FIX] actions 슬롯 없음 - 액션 버튼 불필요
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111)
                )
            )
        }
    ) { innerPadding ->
        // [FIX] 기존 LevelScreen Content를 innerPadding 적용하여 배치
        LevelScreenContent(
            innerPadding = innerPadding,
            currentLevel = currentLevel,
            levelDays = levelDays,
            totalElapsedDaysFloat = totalElapsedDaysFloat,
            startTime = startTime,
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
    totalElapsedDaysFloat: Float,
    startTime: Long?,
    viewModel: Tab03ViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 현재 레벨 카드
                CurrentLevelCard(
                    currentLevel = currentLevel,
                    currentDays = levelDays,
                    elapsedDaysFloat = totalElapsedDaysFloat,
                    startTime = startTime ?: 0L, // [FIX] nullable 처리
                    nextLevel = viewModel.getNextLevel(),
                    progress = viewModel.calculateProgress(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 전체 레벨 리스트
                LevelListCard(
                    currentLevel = currentLevel,
                    currentDays = levelDays
                )

                // [UPDATED] Bottom spacer for breathing room
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// Compose Preview
@androidx.compose.ui.tooling.preview.Preview(
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

