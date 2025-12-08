package kr.sweetapps.alcoholictimer.ui.tab_03

// [REFACTORED] Tab03.kt는 이제 screens.LevelScreen을 재export하는 wrapper입니다.
// 기존 코드는 components와 screens 패키지로 분리되었습니다.
//
// 구조:
// - screens/LevelScreen.kt: 메인 화면 로직 및 광고 처리
// - components/CurrentLevelCard.kt: 현재 레벨 카드
// - components/LevelListCard.kt: 전체 레벨 리스트
// - components/MainLevelCardFrame.kt: 공통 카드 프레임
// - components/LevelDefinitions.kt: 레벨 데이터 정의 (기존 유지)
// - viewmodel/Tab03ViewModel.kt: ViewModel (기존 유지)

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab03ViewModel
import kr.sweetapps.alcoholictimer.ui.tab_03.screens.LevelScreen as LevelScreenImpl

/**
 * Tab03 - 레벨 화면 진입점
 * 하위 호환성을 위해 기존 함수명 유지
 */
@Composable
fun LevelScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: Tab03ViewModel = viewModel()
) {
    LevelScreenImpl(
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

