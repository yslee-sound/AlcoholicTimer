package kr.sweetapps.alcoholictimer.ui.main.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.main.Screen

/**
 * Tab 04: 설정 화면 (현재 빈 페이지)
 * - More: 향후 다른 기능 추가 예정
 * - 기존 습관 설정 기능은 Tab05(더보기)의 '습관 설정' 메뉴로 이동됨
 */
fun NavGraphBuilder.addTab04Graph(navController: NavHostController) {
    composable(Screen.More.route) {
        EmptyTab04Screen()
    }
}

/**
 * [NEW] 빈 Tab04 화면 (향후 확장 예정)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyTab04Screen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.drawer_menu_more),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "준비 중입니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

