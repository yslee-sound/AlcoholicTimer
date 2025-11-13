package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar
import kotlinx.coroutines.delay

/**
 * 공통 스캐폴드: 상단 배너 광고 + 중앙 콘텐츠 + 하단 Bottom Navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    // 전면광고 표시 상태 구독
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ads.AdController.isInterstitialShowingState()

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "🎬 isInterstitialShowing changed: $isInterstitialShowing")
    }

    // 전면광고 종료 시 살짝의 완충을 위해 오버레이를 짧게 유지 후 페이드아웃
    var overlayVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isInterstitialShowing) {
        if (isInterstitialShowing) {
            overlayVisible = true
        } else {
            // 광고 종료 직후 잠깐 유지하여 시스템 기본 애니메이션의 시각적 슬라이드를 완충
            delay(120)
            overlayVisible = false
        }
    }

    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
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

            // 전면광고 표시 중/종료 직후: 검은 오버레이로 부드러운 페이드 처리
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(durationMillis = 120))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
