package com.example.alcoholictimer

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class ActivityDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvActivityTitle: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvSavedMoney: TextView
    private lateinit var tvDetailedInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // UI 요소 초기화
        initViews()

        // 뒤로가기 버튼 설정
        setupBackButton()

        // Intent에서 RecentActivity 데이터 받기
        val activityData = getActivityFromIntent()
        if (activityData != null) {
            displayActivityDetails(activityData)
        } else {
            finish() // 데이터가 없으면 액티비티 종료
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvActivityTitle = findViewById(R.id.tvActivityTitle)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        tvDuration = findViewById(R.id.tvDuration)
        tvResult = findViewById(R.id.tvResult)
        tvSavedMoney = findViewById(R.id.tvSavedMoney)
        tvDetailedInfo = findViewById(R.id.tvDetailedInfo)
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish() // 현재 액티비티 종료하여 이전 화면으로 돌아가기
        }
    }

    private fun getActivityFromIntent(): RecentActivity? {
        return try {
            val jsonString = intent.getStringExtra("activity_json")
            if (!jsonString.isNullOrEmpty()) {
                RecentActivity.fromJson(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityDetailActivity", "Error parsing activity data: ${e.message}")
            null
        }
    }

    private fun displayActivityDetails(activity: RecentActivity) {
        // 활동 제목
        tvActivityTitle.text = activity.title

        // 시작일과 종료일
        tvStartDate.text = "시작일: ${activity.startDate}"
        tvEndDate.text = "종료일: ${activity.endDate}"

        // 지속 시간 (테스트 모드에 따른 단위)
        val timeUnit = when (activity.testMode) {
            Constants.TEST_MODE_SECOND -> "초"
            Constants.TEST_MODE_MINUTE -> "분"
            else -> "일"
        }

        val durationText = if (activity.hours > 0) {
            "${activity.duration}${timeUnit} ${activity.hours}시간"
        } else {
            "${activity.duration}${timeUnit}"
        }
        tvDuration.text = "지속 시간: $durationText"

        // 성공/실패 결과
        if (activity.isSuccess) {
            tvResult.text = "결과: 성공 🎉"
            tvResult.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvResult.text = "결과: 중단됨"
            tvResult.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // 절약 금액
        if (activity.savedMoney > 0) {
            tvSavedMoney.text = "절약 금액: ${activity.savedMoney}만원 💰"
        } else {
            tvSavedMoney.text = "절약 금액: -"
        }

        // 상세 정보
        val detailedInfo = buildString {
            appendLine("📊 활동 상세 정보")
            appendLine()

            // 기간 계산
            val startDateParsed = parseDate(activity.startDate)
            val endDateParsed = parseDate(activity.endDate)

            if (startDateParsed != null && endDateParsed != null) {
                val diffInDays = ((endDateParsed.time - startDateParsed.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffInDays >= 0) {
                    appendLine("📅 실제 경과 일수: ${diffInDays + 1}일")
                }
            }

            appendLine("🎯 목표 달성률: ${if (activity.isSuccess) "100%" else "미완료"}")

            if (activity.testMode != Constants.TEST_MODE_REAL) {
                val testModeText = when (activity.testMode) {
                    Constants.TEST_MODE_SECOND -> "초 단위 테스트"
                    Constants.TEST_MODE_MINUTE -> "분 단위 테스트"
                    else -> "일반 모드"
                }
                appendLine("⚙️ 테스트 모드: $testModeText")
            }

            if (activity.isSuccess) {
                appendLine()
                appendLine("🌟 축하합니다!")
                appendLine("금주 목표를 성공적으로 달성하셨습니다.")
                if (activity.savedMoney > 0) {
                    appendLine("총 ${activity.savedMoney}만원을 절약하셨습니다!")
                }
            } else {
                appendLine()
                appendLine("💪 다음에는 더 좋은 결과가 있을 거예요!")
                appendLine("포기하지 마시고 다시 도전해보세요.")
            }
        }

        tvDetailedInfo.text = detailedInfo
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
