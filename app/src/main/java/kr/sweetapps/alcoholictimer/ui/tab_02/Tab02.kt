// [NEW] Tab02 리팩토링: 기록 화면을 tab_02 구조로 리팩토링
package kr.sweetapps.alcoholictimer.ui.tab_02

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.RecordsScreen
import kr.sweetapps.alcoholictimer.ui.tab_02.screens.DiaryEntry // [NEW] DiaryEntry import

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
 * - ViewModel을 통한 상태 관리 (향후 확장 가능)
 */
@Composable
fun Tab02Screen(
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    onNavigateToAllRecords: () -> Unit = {},
    onAddRecord: () -> Unit = {},
    onDiaryClick: (DiaryEntry) -> Unit = {}, // [NEW] 일기 클릭 콜백
    viewModel: Tab02ViewModel = viewModel()
) {
    // [NEW] 현재는 RecordsScreen을 그대로 사용하지만,
    // 향후 필요시 ViewModel의 상태를 활용하여 확장 가능
    RecordsScreen(
        externalRefreshTrigger = 0,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAllRecords = onNavigateToAllRecords,
        onAddRecord = onAddRecord,
        onDiaryClick = onDiaryClick // [NEW] 일기 클릭 콜백 전달
    )
}

