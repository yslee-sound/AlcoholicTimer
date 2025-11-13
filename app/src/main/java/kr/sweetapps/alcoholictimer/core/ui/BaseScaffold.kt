package kr.sweetapps.alcoholictimer.core.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar

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

    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 기본 UI
            Column(modifier = Modifier.fillMaxSize()) {
                // 전면광고 표시 중이 아닐 때만 배너 영역 렌더링
                if (!isInterstitialShowing) {
                    // 상단 배너
                    Surface(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        AdmobBanner(modifier = Modifier.fillMaxWidth())
                    }

                    // 구분선
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                }

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

            // 전면광고 표시 중일 때: 전체 화면을 검은색으로 덮음 (전면광고 아래 앱 UI 숨김)
            if (isInterstitialShowing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}
