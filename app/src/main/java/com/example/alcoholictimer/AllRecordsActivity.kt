package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.alcoholictimer.components.AllRecordsScreen
import com.example.alcoholictimer.ui.theme.AlcoholicTimerTheme
import com.example.alcoholictimer.utils.SobrietyRecord

class AllRecordsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AllRecordsActivity"
    }

    // 상세 화면에서의 삭제 완료 등을 반영하기 위한 외부 새로고침 트리거
    private var externalRefreshTriggerState by mutableStateOf(0)

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "DetailActivity RESULT_OK 수신 → 리스트 새로고침 트리거 증가")
            externalRefreshTriggerState++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AlcoholicTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AllRecordsScreen(
                        externalRefreshTrigger = externalRefreshTriggerState,
                        onNavigateBack = { finish() },
                        onNavigateToDetail = { record -> handleRecordClick(record) }
                    )
                }
            }
        }
    }

    private fun handleRecordClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 기록 클릭 시작 =====")
        Log.d(TAG, "기록 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")

        try {
            // 데이터 유효성 검사
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }

            // targetDays가 0이면 기본값으로 설정
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays

            // DetailActivity로 이동 (결과 수신용 런처 사용)
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
