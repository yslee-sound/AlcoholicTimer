// Tab01.kt

package kr.sweetapps.alcoholictimer.ui.tab_01

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel

/**
 * Tab01 Start 화면 Composable
 */
@Composable
fun Tab01StartScreen(
    gateNavigation: Boolean = true,
    onStart: (Int) -> Unit = {},
    viewModel: Tab01ViewModel = viewModel(viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity)
) {
    // [FIX] 화면 전체를 강제로 새로고침하기 위한 설정 감지
    val configuration = LocalConfiguration.current

    // [FIX] 설정(언어)이 바뀌면 StartScreen을 파괴하고 다시 만듭니다.
    key(configuration) {
        StartScreen(
            gateNavigation = gateNavigation,
            onStart = onStart
        )
    }
}

@Composable
fun Tab01RunScreen(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null,
    viewModel: Tab01ViewModel = viewModel(viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity)
) {
    // Run 화면도 동일하게 적용
    val configuration = LocalConfiguration.current

    key(configuration) {
        RunScreenComposable(
            onRequestQuit = onRequestQuit,
            onCompletedNavigateToDetail = onCompletedNavigateToDetail,
            onRequireBackToStart = onRequireBackToStart,
            viewModel = viewModel
        )
    }
}