package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.Screen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar
import kotlinx.coroutines.delay

/**
 * 공통 스캐폴드: 상단 배너 광고 + 중앙 콘텐츠 + 하단 Bottom Navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    contentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // 전면광고 표시 상태 구독
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ads.AdController.isInterstitialShowingState()

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "🎬 isInterstitialShowing changed: $isInterstitialShowing")
    }

    // 전면광고 종료 후 짧은 홀드로 시각적 슬라이드를 완충 (애니메이션 제거)
    var overlayHoldActive by remember { mutableStateOf(false) }
    LaunchedEffect(isInterstitialShowing) {
        if (!isInterstitialShowing) {
            overlayHoldActive = true
            delay(120)
            overlayHoldActive = false
        }
    }
    val overlayVisible = isInterstitialShowing || overlayHoldActive

    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        // Observe nav back stack to allow per-route content background overrides (e.g., Run screen)
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Box(modifier = Modifier.fillMaxSize()) {
            // 기본 UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // 배너: 항상 렌더링하고 내부에서 공간 예약하여 레이아웃 시프트 방지
                AdmobBanner(
                    modifier = Modifier.fillMaxWidth(),
                    reserveSpaceWhenDisabled = true
                )

                // 구분선도 항상 유지하여 상단 영역 높이 변화를 없앰
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // 중앙 콘텐츠
                val effectiveBg = when (currentRoute) {
                    Screen.Run.route -> Color(0xFFEEEDE9)
                    else -> contentBackground ?: MaterialTheme.colorScheme.background
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(effectiveBg)
                ) {
                    content()
                }

                // 하단 구분선
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom Navigation (제스처/3버튼과 겹치지 않도록 패딩)
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    BottomNavBar(navController = navController)
                }
            }

            // 전면광고 표시 중/종료 직후: 검은 오버레이로 즉시 덮기 (애니메이션 없음)
            if (overlayVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
