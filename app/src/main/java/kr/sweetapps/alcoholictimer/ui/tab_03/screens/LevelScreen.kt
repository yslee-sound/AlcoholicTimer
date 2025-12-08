package kr.sweetapps.alcoholictimer.ui.tab_03.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import kr.sweetapps.alcoholictimer.ui.common.LocalSafeContentPadding
import kr.sweetapps.alcoholictimer.ui.tab_03.Tab03ViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.components.CurrentLevelCard
import kr.sweetapps.alcoholictimer.ui.tab_03.components.LevelListCard

/**
 * Tab03 - 레벨 화면
 * 사용자의 금주 레벨 진행 상황을 보여주는 메인 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: Tab03ViewModel = viewModel()
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

    CompositionLocalProvider(LocalSafeContentPadding provides PaddingValues(bottom = 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        startTime = startTime,
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

                    // [UPDATED] Bottom spacer for breathing room (changed from padding to Spacer)
                    Spacer(modifier = Modifier.height(100.dp))
                }
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

