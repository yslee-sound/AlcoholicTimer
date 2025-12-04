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
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.Screen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.core.ui.components.BottomNavBar
import kotlinx.coroutines.delay

/**
 * 공통 ?�캐?�드: ?�단 배너 광고 + 중앙 콘텐�?+ ?�단 Bottom Navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    contentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // ?�면광고 ?�시 ?�태 구독
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ui.ad.AdController.isInterstitialShowingState()

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "?�� isInterstitialShowing changed: $isInterstitialShowing")
    }

    // ?�면광고 종료 ??짧�? ?�?�로 ?�각???�라?�드�??�충 (?�니메이???�거)
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
                // [REMOVED] 배너 광고 ?�거 (2025-12-01)
                // 기존 코드:
                // AdmobBanner(modifier = Modifier.fillMaxWidth(), reserveSpaceWhenDisabled = true)
                // HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // 중앙 콘텐�?
                val effectiveBg = when (currentRoute) {
                    Screen.Run.route -> Color(0xFFEEEDE9)
                    else -> contentBackground ?: Color.White
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(effectiveBg)
                ) {
                    content()
                }

                // ?�단 구분??
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom Navigation (?�스�?3버튼�?겹치지 ?�도�??�딩)
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    BottomNavBar(navController = navController)
                }
            }

            // ?�면광고 ?�시 �?종료 직후: 검?� ?�버?�이�?즉시 ??�� (?�니메이???�음)
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
