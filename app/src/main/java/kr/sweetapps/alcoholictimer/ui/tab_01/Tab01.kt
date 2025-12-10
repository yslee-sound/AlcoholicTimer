// [NEW] Tab01 리팩토링: Start/Run 화면을 tab_01 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_01

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel

/**
 * Tab01 Start 화면 Composable
 * ViewModel을 Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
 */
@Composable
fun Tab01StartScreen(
    gateNavigation: Boolean = true,
    onStart: (Int) -> Unit = {},
    viewModel: Tab01ViewModel = viewModel(viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity)
) {
    StartScreen(
        gateNavigation = gateNavigation,
        onStart = onStart
    )
}

/**
 * Tab01 Run 화면 Composable
 * ViewModel을 Activity Scope로 변경하여 탭 전환 시에도 동일한 인스턴스 유지
 */
@Composable
fun Tab01RunScreen(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null,
    viewModel: Tab01ViewModel = viewModel(viewModelStoreOwner = androidx.activity.compose.LocalActivity.current as ComponentActivity)
) {
    RunScreenComposable(
        onRequestQuit = onRequestQuit,
        onCompletedNavigateToDetail = onCompletedNavigateToDetail,
        onRequireBackToStart = onRequireBackToStart,
        viewModel = viewModel // Pass ViewModel to RunScreenComposable
    )
}

