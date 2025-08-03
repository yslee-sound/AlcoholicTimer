package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 금주 완료 후 결과 요약을 보여주는 액티비티
 */
class RecordSummaryActivity : BaseActivity() {

    private var record: SobrietyRecord? = null
    private val TAG = "RecordSummaryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 기록화면 시작")

        // 이전 화면에서 전달받은 기록 ID 가져오기
        val recordId = intent.getLongExtra("record_id", -1L)
        Log.d(TAG, "onCreate: recordId=$recordId")

        if (recordId == -1L) {
            // ID가 없으면 메인 화면으로 이동
            Toast.makeText(this, "기록 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Record ID not found in intent")
            finish()
            return
        }

        // 해당 ID의 기록 데이터 불러오기
        record = loadRecord(recordId)

        if (record == null) {
            Toast.makeText(this, "해당 기록을 불러올 수 없습니다.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to load record with id: $recordId")
        } else {
            Log.d(TAG, "Record loaded: ${record?.startDate} to ${record?.endDate}, completed=${record?.isCompleted}")
        }
    }

    override fun setupContentView() {
        Log.d(TAG, "setupContentView: 시작")

        try {
            // BaseActivity에서 상속받은 contentFrame에 레이아웃 추가
            val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
            if (contentFrame == null) {
                Log.e(TAG, "contentFrame not found")
                return
            }

            val view = LayoutInflater.from(this).inflate(R.layout.activity_record_summary, contentFrame, true)
            Log.d(TAG, "Layout inflated successfully")

            // UI 요소 초기화 - 새로 추가된 요소들 포함
            val tvRecordScreenTitle = view.findViewById<TextView>(R.id.tvRecordScreenTitle)
            val tvSummaryTitle = view.findViewById<TextView>(R.id.tvSummaryTitle)
            val tvPeriod = view.findViewById<TextView>(R.id.tvPeriod)
            val tvDuration = view.findViewById<TextView>(R.id.tvDuration)
            val tvTargetDuration = view.findViewById<TextView>(R.id.tvTargetDuration)
            val tvAchievedLevel = view.findViewById<TextView>(R.id.tvAchievedLevel)
            val tvCompletionStatus = view.findViewById<TextView>(R.id.tvCompletionStatus)
            val tvCompletionMessage = view.findViewById<TextView>(R.id.tvCompletionMessage)
            val btnNewChallenge = view.findViewById<Button>(R.id.btnNewChallenge)

            if (tvRecordScreenTitle == null || tvSummaryTitle == null || tvPeriod == null || btnNewChallenge == null) {
                Log.e(TAG, "One or more UI elements not found in layout")
                return
            }

            // 기본 타이틀은 항상 표시
            tvRecordScreenTitle.text = "기록 요약"

            // 레코드가 있는 경우에만 데이터 표시
            if (record != null) {
                // 제목 설정
                if (record!!.isCompleted) {
                    tvSummaryTitle.text = "🎉 목표 달성을 축하합니다! 🎉"
                    tvCompletionStatus?.text = "목표 완료!"
                    tvCompletionMessage?.text = "설정한 목표를 성공적으로 달성했습니다!"
                } else {
                    tvSummaryTitle.text = "금주 기록 요약"
                    tvCompletionStatus?.text = "중도 중단"
                    tvCompletionMessage?.text = "다음에는 더 좋은 결과가 있을 거예요!"
                }

                // 날짜 포맷 설정
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())
                val startDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(record!!.startDate) ?: Date())
                val endDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(record!!.endDate) ?: Date())

                // 기간 표시
                tvPeriod.text = "시작: $startDate\n종료: $endDate"

                // 목표 시간 표시
                val timeUnit = getTimeUnitText()
                tvTargetDuration?.text = "${record!!.duration}$timeUnit"

                // 달성 시간을 테스트 모드에 따라 다르게 표시
                val durationText = formatDurationByTestMode(record!!.startDate, record!!.endDate)
                tvDuration?.text = durationText

                // 달성 레벨 표시
                val levelText = if (record!!.levelTitle.isNotEmpty()) {
                    "레벨 ${record!!.achievedLevel} - ${record!!.levelTitle}"
                } else {
                    "레벨 ${record!!.achievedLevel}"
                }
                tvAchievedLevel?.text = levelText

            } else {
                // 레코드가 없는 경우 기본 텍스트 설정
                tvSummaryTitle.text = "기록 정보 없음"
                tvPeriod.text = "기록 정보를 불러올 수 없습니다"
                tvDuration?.text = "0${getTimeUnitText()}"
                tvTargetDuration?.text = "0${getTimeUnitText()}"
                tvAchievedLevel?.text = "레벨 정보 없음"
                tvCompletionStatus?.text = "정보 없음"
                tvCompletionMessage?.text = "기록을 확인할 수 없습니다"
            }

            // 새로운 도전 버튼 클릭 리스너 설정
            btnNewChallenge.setOnClickListener {
                // MainActivity로 이동
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP  // 스택의 MainActivity 위의 모든 액티비티 제거
                startActivity(intent)
                finish()
            }

            Log.d(TAG, "setupContentView: 완료")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up content view: ${e.message}", e)
        }
    }

    /**
     * 현재 테스트 모드에 맞는 시간 단위 텍스트 반환
     */
    private fun getTimeUnitText(): String {
        return when (Constants.currentTestMode) {
            Constants.TEST_MODE_SECOND -> "초"
            Constants.TEST_MODE_MINUTE -> "분"
            else -> "일"
        }
    }

    /**
     * 테스트 모드에 따라 기간을 다르게 포맷팅
     */
    private fun formatDurationByTestMode(startDateStr: String, endDateStr: String): String {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDate = dateFormat.parse(startDateStr) ?: Date()
            val endDate = dateFormat.parse(endDateStr) ?: Date()

            val durationMillis = endDate.time - startDate.time

            return when (Constants.currentTestMode) {
                Constants.TEST_MODE_SECOND -> {
                    // 초 단위 테스트 모드: 0분 00초 형식
                    val totalSeconds = (durationMillis / 1000).toInt()
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    "${minutes}분 ${String.format("%02d", seconds)}초"
                }
                Constants.TEST_MODE_MINUTE -> {
                    // 분 단위 테스트 모드: 0시간 00분 형식
                    val totalMinutes = (durationMillis / (1000 * 60)).toInt()
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    "${hours}시간 ${String.format("%02d", minutes)}분"
                }
                else -> {
                    // 실제 모드 (일 단위): 0일 00시간 형식
                    val totalHours = (durationMillis / (1000 * 60 * 60)).toInt()
                    val days = totalHours / 24
                    val hours = totalHours % 24
                    "${days}일 ${String.format("%02d", hours)}시간"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting duration: ${e.message}", e)
            return "기간 계산 오류"
        }
    }

    /**
     * ID로 기록 불러오기
     */
    private fun loadRecord(recordId: Long): SobrietyRecord? {
        try {
            val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
            val recordsJson = sharedPref.getString("records", "[]")
            val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]")

            return records.find { it.id == recordId }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading record: ${e.message}", e)
            return null
        }
    }
}
