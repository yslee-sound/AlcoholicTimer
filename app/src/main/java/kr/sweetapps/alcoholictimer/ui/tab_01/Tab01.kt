// [NEW] Tab01 리팩토링: Start/Run 화면을 tab_01 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_01

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.RunScreenComposable
import kr.sweetapps.alcoholictimer.ui.tab_01.screens.StartScreen

/**
 * Tab01 Start 화면 Composable
 */
@Composable
fun Tab01StartScreen(
    gateNavigation: Boolean = true,
    onStart: (Int) -> Unit = {},
    viewModel: Tab01ViewModel = viewModel()
) {
    StartScreen(
        gateNavigation = gateNavigation,
        onStart = onStart
    )
}

/**
 * Tab01 Run 화면 Composable
 */
@Composable
fun Tab01RunScreen(
    onRequestQuit: (() -> Unit)? = null,
    onCompletedNavigateToDetail: ((String) -> Unit)? = null,
    onRequireBackToStart: (() -> Unit)? = null,
    viewModel: Tab01ViewModel = viewModel()
) {
    RunScreenComposable(
        onRequestQuit = onRequestQuit,
        onCompletedNavigateToDetail = onCompletedNavigateToDetail,
        onRequireBackToStart = onRequireBackToStart
    )
}

