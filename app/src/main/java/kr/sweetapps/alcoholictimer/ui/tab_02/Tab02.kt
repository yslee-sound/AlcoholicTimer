// [NEW] Tab02 리팩토링: 기록 화면을 tab_02 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_02

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.ui.common.BaseActivity
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.Tab02ViewModel
import kr.sweetapps.alcoholictimer.ui.theme.AlcoholicTimerTheme

/**
 * [NEW] Tab02 기록 화면 Activity
 * - RecordsScreen을 감싸는 Activity 래퍼
 * - BaseActivity를 상속받아 통일된 UI 제공
 */
class RecordsActivity : BaseActivity() {
    override fun getScreenTitleResId(): Int = R.string.records_title

    @Deprecated("Use getScreenTitleResId() instead for proper localization support")
    override fun getScreenTitle(): String = getString(R.string.records_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(content = { Tab02Screen() })
        }
    }
}

/**
 * [NEW] Tab02 메인 화면 Composable
 * - RecordsScreen을 래핑하여 일관된 구조 제공
 * - DiaryViewModel을 통해 Room DB 데이터를 관찰하고 전달
 */
@Composable
fun Tab02Screen(
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onNavigateToAllDiaries: () -> Unit = {}, // [NEW] 모든 일기 보기 콜백 추가
    onAddRecord: () -> Unit = {},
    onDiaryClick: (kr.sweetapps.alcoholictimer.data.room.DiaryEntity) -> Unit = {}, // [UPDATED] DiaryEntity 사용
    viewModel: Tab02ViewModel = viewModel()
) {
    // [NEW] DiaryViewModel을 통해 Room DB의 일기 데이터를 실시간으로 관찰
    val diaryViewModel: kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel.DiaryViewModel = viewModel()
    val allDiaries by diaryViewModel.uiState.collectAsState()

    // [NEW] 최신 3개의 일기만 추출 (이미 timestamp 내림차순 정렬됨)
    val recentDiaries = remember(allDiaries) {
        allDiaries.take(3)
    }

    RecordsScreen(
        externalRefreshTrigger = 0,
        recentDiaries = recentDiaries, // [NEW] Room DB 데이터 전달
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAllRecords = onNavigateToAllRecords,
        onNavigateToAllDiaries = onNavigateToAllDiaries, // [NEW] 모든 일기 보기 콜백 전달
        onAddRecord = onAddRecord,
        onDiaryClick = onDiaryClick // [NEW] 일기 클릭 콜백 전달
    )
}

/**
 * [NEW] Tab02Screen 프리뷰
 * - RecordsScreen을 있는 그대로 보여주는 프리뷰
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Tab02ScreenPreview() {
    AlcoholicTimerTheme {
        Tab02Screen()
    }
}
