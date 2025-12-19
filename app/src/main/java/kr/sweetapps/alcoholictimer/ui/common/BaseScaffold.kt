package kr.sweetapps.alcoholictimer.ui.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.ui.main.Screen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import kr.sweetapps.alcoholictimer.ui.components.BottomNavBar
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.stringResource

/**
 * Common Scaffold: Top banner ad + center content + bottom navigation
 */
@Composable
fun BaseScaffold(
    navController: NavHostController,
    rootNavController: NavHostController? = null, // [NEW] Success 화면 이동용
    contentBackground: Color? = null,
    content: @Composable () -> Unit
) {
    // Subscribe to interstitial ad showing state
    val isInterstitialShowing = kr.sweetapps.alcoholictimer.ui.ad.AdController.isInterstitialShowingState()
    val overlayHoldActive = remember { mutableStateOf(false) }
    val previousInterstitialState = remember { mutableStateOf(isInterstitialShowing) }

    LaunchedEffect(isInterstitialShowing) {
        Log.d("BaseScaffold", "Ad isInterstitialShowing changed: $isInterstitialShowing")
        if (!isInterstitialShowing && previousInterstitialState.value) {
            overlayHoldActive.value = true
            delay(120)
            overlayHoldActive.value = false
        }
        previousInterstitialState.value = isInterstitialShowing
    }

    val overlayVisible = isInterstitialShowing || overlayHoldActive.value

    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        // Observe nav back stack to allow per-route content background overrides (e.g., Run screen)
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        // [NEW] 탭 2(Records)에서만 표시되는 상단 타이틀 바 결정
        // [FIX] 탭 3(Level), 탭 4(More)는 제외 - 각 Screen이 자체 TopAppBar를 가지므로 중복 방지
        val topTitle: String? = when {
            // 탭 2 그룹: Records 및 관련 라우트 (AllRecords, Detail은 제외 - 전체 화면 모드)
            currentRoute == Screen.Records.route || currentRoute == Screen.AddRecord.route -> stringResource(R.string.records_title)
            // [REMOVED] 탭 3, 탭 4는 제거 - 각 Screen이 자체 TopAppBar 사용
            else -> null
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Base UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // [NEW] 상단 타이틀 바: 탭 2/3/4에서만 보이도록 함
                if (topTitle != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = topTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111),
                            modifier = Modifier.weight(1f)
                        )

                        // 우측 아이콘 공간(추후 아이콘 추가 가능)
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Center content
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

                // Bottom divider
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom Navigation (always visible inside tab host)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    BottomNavBar(
                        navController = navController,
                        rootNavController = rootNavController
                    )
                }
            }

            // Interstitial ad showing or just after close: black overlay (no animation)
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
