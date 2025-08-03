package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 금주 완료 후 결과 요약을 보여주는 액티비티
 */
class RecordSummaryActivity : AppCompatActivity() {

    private var record: SobrietyRecord? = null
    private val TAG = "RecordSummaryActivity"

    // UI 요소들
    private lateinit var tvDuration: TextView
    private lateinit var tvPeriod: TextView
    private lateinit var tvAchievementRate: TextView
    private lateinit var tvSavedMoney: TextView
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_summary)

        Log.d(TAG, "onCreate: 기록화면 시작")

        // UI 요소 초기화
        tvDuration = findViewById(R.id.tvDuration)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvAchievementRate = findViewById(R.id.tvAchievementRate)
        tvSavedMoney = findViewById(R.id.tvSavedMoney)
        btnConfirm = findViewById(R.id.btnConfirm)

        // 뒤로가기 버튼 설정
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            // 메인 화면으로 이동 (앱 종료 방지)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 확인 버튼 클릭 시 메인 화면으로 이동
        btnConfirm.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

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
            Log.d(TAG, "Record loaded successfully: ${record?.startDate} to ${record?.endDate}, completed=${record?.isCompleted}, duration=${record?.duration}")
            // 기록 데이터를 화면에 표시
            displayRecordData()
        }
    }

    /**
     * 기록 데이터를 화면에 표시
     */
    private fun displayRecordData() {
        val currentRecord = record ?: return

        // 테스트 모드인지 확인
        val testMode = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        val isTestMode = testMode == Constants.TEST_MODE_SECOND || testMode == Constants.TEST_MODE_MINUTE

        // 금주 기간 표시 - 테스트 모드에서는 설정된 목표 기간을 표시
        val durationValue = if (isTestMode) {
            // 테스트 모드에서는 설정한 목표 시간(targetDays)을 사용
            currentRecord.duration.toLong()
        } else {
            // 실제 모드에서는 달성한 기간 사용
            currentRecord.achievedDays.toLong()
        }

        Log.d(TAG, "displayRecordData: 테스트 모드=$isTestMode, 목표 기간=${currentRecord.duration}, 달성 기간=${currentRecord.achievedDays}, 표시할 값=$durationValue")

        val duration = formatDuration(durationValue)
        tvDuration.text = duration

        // 금주 일정 표시
        tvPeriod.text = "${currentRecord.startDate} ~ ${currentRecord.endDate}"

        // 달성률 표시
        val achievementRate = if (currentRecord.isCompleted) "100%" else {
            val percentage = (currentRecord.achievedDays.toDouble() / currentRecord.duration.toDouble() * 100).toInt()
            "$percentage%"
        }
        tvAchievementRate.text = achievementRate
        tvAchievementRate.setTextColor(
            if (currentRecord.isCompleted)
                resources.getColor(android.R.color.holo_green_dark, null)
            else
                resources.getColor(android.R.color.holo_orange_dark, null)
        )

        // 절약 금액 계산 및 표시
        val savedMoney = calculateSavedMoney(currentRecord.duration.toLong())
        tvSavedMoney.text = savedMoney
    }

    /**
     * 지속 시간을 포맷팅 (테스트 모드에 따라)
     */
    private fun formatDuration(durationValue: Long): String {
        val testMode = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        return when (testMode) {
            Constants.TEST_MODE_SECOND -> {
                // 초 단위 테스트 모드: 0분 00초 형식
                val totalSeconds = durationValue
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                "${minutes}분 ${String.format("%02d", seconds)}초"
            }
            Constants.TEST_MODE_MINUTE -> {
                // 분 단위 테스트 모드: 0시간 00분 형식
                val totalMinutes = durationValue
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                "${hours}시간 ${String.format("%02d", minutes)}분"
            }
            else -> {
                // 실제 모드 (일 단위): 0일 형식
                val totalDays = durationValue
                "${totalDays}일"
            }
        }
    }

    /**
     * 절약 금액 계산 (임시로 1일당 10,000원으로 계산)
     */
    private fun calculateSavedMoney(durationValue: Long): String {
        val testMode = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        val dailyAmount = 10000 // 1일당 10,000원

        val days = when (testMode) {
            Constants.TEST_MODE_SECOND -> durationValue // 초 단위 테스트: 1초 = 1일로 계산
            Constants.TEST_MODE_MINUTE -> durationValue // 분 단위 테스트: 1분 = 1일로 계산
            else -> durationValue // 실제 모드: 실제 일수
        }

        val totalAmount = days * dailyAmount
        return when {
            totalAmount >= 10000 -> "${totalAmount / 10000}만원"
            totalAmount >= 1000 -> "${totalAmount / 1000}천원"
            else -> "${totalAmount}원"
        }
    }

    /**
     * 기록 데이터를 SharedPreferences에서 불러오기
     */
    private fun loadRecord(recordId: Long): SobrietyRecord? {
        Log.d(TAG, "loadRecord: 기록 ID $recordId 불러오기 시도")

        try {
            // 기록 저장용 SharedPreferences에서 모든 기록 불러오기
            val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
            val recordsJson = sharedPref.getString("records", "[]")
            Log.d(TAG, "로드된 기록 JSON: $recordsJson")

            // JSON 배열을 SobrietyRecord 객체 리스트로 변환
            val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]")
            Log.d(TAG, "파싱된 기록 수: ${records.size}")

            // 해당 ID를 가진 기록 찾기
            val foundRecord = records.find { it.id == recordId }

            if (foundRecord != null) {
                Log.d(TAG, "기록 찾음: $foundRecord")
                return foundRecord
            } else {
                Log.e(TAG, "ID가 ${recordId}인 기록을 찾을 수 없음")

                // 디버깅을 위해 모든 기록의 ID 출력
                records.forEach {
                    Log.d(TAG, "기존 기록 ID: ${it.id}, 달성일수: ${it.achievedDays}")
                }

                // 기록을 찾을 수 없으면 테스트용 기록 생성
                // 이는 디버깅용으로만 사용하고, 실제 앱에서는 제거해야 함
                val testRecord = createHardcodedTestRecord(recordId, 3) // 3초로 고정
                Log.d(TAG, "테스트용 기록 생성: $testRecord")
                return testRecord
            }
        } catch (e: Exception) {
            Log.e(TAG, "기록 로딩 중 오류 발생", e)
            return null
        }
    }

    /**
     * 테스트용 하드코딩된 기록 생성 (임시 디버깅용)
     */
    private fun createHardcodedTestRecord(recordId: Long, targetDays: Int): SobrietyRecord {
        val testMode = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(System.currentTimeMillis() - 10000))
        val endDateStr = dateFormat.format(Date(System.currentTimeMillis()))

        return SobrietyRecord(
            id = recordId,
            startDate = startDateStr,
            endDate = endDateStr,
            duration = targetDays, // 설정 목표일 (3초)
            achievedDays = targetDays, // 달성일도 목표일과 동일하게
            achievedLevel = 1,
            levelTitle = "첫걸음",
            isCompleted = true
        )
    }

    /**
     * 실제 지속 시간 계산 (테스트 모드에 따라)
     */
    private fun calculateActualDuration(startTime: Long, endTime: Long): Long {
        val testMode = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)

        val durationMs = endTime - startTime
        Log.d(TAG, "calculateActualDuration - testMode: $testMode, durationMs: $durationMs")

        return when (testMode) {
            Constants.TEST_MODE_SECOND -> {
                // 초 단위 테스트: 경과한 실제 초 수를 반환
                val seconds = durationMs / 1000
                Log.d(TAG, "TEST_MODE_SECOND - returning $seconds seconds")
                seconds
            }
            Constants.TEST_MODE_MINUTE -> {
                // 분 단위 테스트: 경과한 실제 분 수를 반환
                val minutes = durationMs / (1000 * 60)
                Log.d(TAG, "TEST_MODE_MINUTE - returning $minutes minutes")
                minutes
            }
            else -> {
                // 실제 모드: 경과한 실제 일 수를 반환
                val days = durationMs / (1000 * 60 * 60 * 24)
                Log.d(TAG, "TEST_MODE_REAL - returning $days days")
                days
            }
        }
    }
}
