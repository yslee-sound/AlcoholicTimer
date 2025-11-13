package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
    AlcoholicTimerTheme(darkTheme = false, applySystemBars = true) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 배너
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                AdmobBanner(modifier = Modifier.fillMaxWidth().heightIn(min = LayoutConstants.BANNER_MIN_HEIGHT))
            }

            // 구분선
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
    }
}
