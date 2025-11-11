package kr.sweetapps.alcoholictimer.feature.records

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.feature.detail.DetailActivity
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.stringResource
import kr.sweetapps.alcoholictimer.R

class AllRecordsActivity : BaseActivity() {

    companion object { private const val TAG = "AllRecordsActivity" }

    // Compose 위임 대신 Compose 상태로 유지하여 재조합을 유도
    private val externalRefreshTriggerState = mutableIntStateOf(0)

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "DetailActivity RESULT_OK 수신 → 리스트 새로고침 트리거 증가")
            externalRefreshTriggerState.intValue = externalRefreshTriggerState.intValue + 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val showDeleteAll = remember { mutableStateOf(false) }
            BaseScreen(
                showBackButton = true,
                onBackClick = { finish() },
                topBarActions = {
                    IconButton(onClick = { showDeleteAll.value = true }) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.all_records_delete_title))
                    }
                }
            , content = {
                kr.sweetapps.alcoholictimer.feature.records.components.AllRecordsScreen(
                    externalRefreshTrigger = externalRefreshTriggerState.intValue,
                    onNavigateBack = { finish() },
                    onNavigateToDetail = { record -> handleRecordClick(record) },
                    externalDeleteDialog = showDeleteAll
                )
            })
        }
    }

    override fun getScreenTitleResId(): Int = R.string.all_records_title
    override fun getScreenTitle(): String = getString(R.string.all_records_title)

    private fun handleRecordClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 기록 클릭 시작 =====")
        Log.d(TAG, "기록 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")

        try {
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays
            val intent = Intent(this@AllRecordsActivity, DetailActivity::class.java).apply {
                putExtra("start_time", record.startTime)
                putExtra("end_time", record.endTime)
                putExtra("target_days", safeTargetDays.toFloat())
                putExtra("actual_days", record.actualDays)
                putExtra("is_completed", record.isCompleted)
            }
            Log.d(TAG, "DetailActivity 호출(결과 대기)...")
            detailLauncher.launch(intent)
            Log.d(TAG, "===== 기록 클릭 종료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "DetailActivity 화면 이동 중 오류", e)
        }
    }
}
