package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.alcoholictimer.components.RecordsScreen
import com.example.alcoholictimer.utils.SobrietyRecord

class RecordsActivity : BaseActivity() {

    // 디버깅용 태그
    companion object {
        private const val TAG = "RecordsActivity"
    }

    // refreshTrigger를 Activity 레벨 primitive state로 전환
    private var refreshTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen(
                    externalRefreshTrigger = refreshTrigger,
                    onNavigateToDetail = { record -> handleCardClick(record) },
                    onNavigateToAllRecords = { navigateToAllRecords() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 나타날 때마다 데이터를 새로고침
        Log.d(TAG, "onResume: 기록 화면이 다시 나타남 - 데이터 새로고침")
        // refreshTrigger 증가시켜 강제 새로고침
        refreshTrigger++
    }

    override fun getScreenTitle(): String = "금주 기록"

    private fun handleCardClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 카드 클릭 시작 =====")
        Log.d(TAG, "카드 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")
        Log.d(TAG, "startTime=${record.startTime}, endTime=${record.endTime}")
        Log.d(TAG, "isCompleted=${record.isCompleted}")

        try {
            // 데이터 유효성 검사 (더 관대하게)
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }

            Log.d(TAG, "Intent 생성 시작...")

            // targetDays가 0이면 기본값으로 설정
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays

            // DetailActivity로 이동
            val intent = Intent(this@RecordsActivity, DetailActivity::class.java)
            intent.putExtra("start_time", record.startTime)
            intent.putExtra("end_time", record.endTime)
            intent.putExtra("target_days", safeTargetDays.toFloat())
            intent.putExtra("actual_days", record.actualDays)
            intent.putExtra("is_completed", record.isCompleted)

            Log.d(TAG, "Intent 데이터 전달: targetDays=$safeTargetDays, actualDays=${record.actualDays}")
            Log.d(TAG, "DetailActivity 호출...")
            startActivity(intent)
            Log.d(TAG, "startActivity 호출 완료")
            Log.d(TAG, "===== 카드 클릭 종료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "CardDetail 화면 이동 중 오류", e)
            Log.e(TAG, "오류 스택트레이스: ${e.stackTraceToString()}")
        }
    }

    private fun navigateToAllRecords() {
        Log.d(TAG, "모든 기록 화면으로 이동")
        val intent = Intent(this@RecordsActivity, AllRecordsActivity::class.java)
        startActivity(intent)
    }
}
