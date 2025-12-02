// [NEW] Tab01 리팩토링: Start/Run 화면을 tab_01 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_01

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen

/**
 * [NEW] Tab01 Start 화면 Composable
 * - StartScreen을 래핑하여 일관된 구조 제공
 * - ViewModel을 통한 상태 관리 (향후 확장 가능)
 */
@Composable
fun Tab01StartScreen(
    gateNavigation: Boolean = true,
    onStart: (Int) -> Unit = {},
    viewModel: Tab01ViewModel = viewModel()
) {
    // [NEW] 현재는 StartScreen을 그대로 사용하지만,
    // 향후 필요시 ViewModel의 상태를 활용하여 확장 가능
    StartScreen(
        gateNavigation = gateNavigation,
        onStart = onStart
    )
}

/**
 * [NEW] Tab01 Run 화면 Composable
 * - RunScreenComposable을 래핑하여 일관된 구조 제공
 * - ViewModel을 통한 상태 관리 (향후 확장 가능)
 */
@Composable
fun Tab01RunScreen(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null,
    viewModel: Tab01ViewModel = viewModel()
) {
    // [NEW] 현재는 RunScreenComposable을 그대로 사용하지만,
    // 향후 필요시 ViewModel의 상태를 활용하여 확장 가능
    RunScreenComposable(
        onRequestQuit = onRequestQuit,
        onCompletedNavigateToDetail = onCompletedNavigateToDetail,
        onRequireBackToStart = onRequireBackToStart
    )
}

