package com.example.alcoholictimer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

class RecordsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        val tvWeeklyCount = findViewById<TextView>(R.id.tvWeeklyCount)
        val tvMonthlyCount = findViewById<TextView>(R.id.tvMonthlyCount)
        val tvTotalCount = findViewById<TextView>(R.id.tvTotalCount)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)

        // SharedPreferences에서 시작일 불러오기
        val sharedPref = getSharedPreferences("AlcoholicPrefs", MODE_PRIVATE)
        val startDateStr = sharedPref.getString("start_date", null)

        if (startDateStr != null) {
            val startDate = LocalDate.parse(startDateStr)
            val today = LocalDate.now()

            // 전체 금주 일수
            val totalDays = ChronoUnit.DAYS.between(startDate, today)

            // 이번 주 금주 일수
            val weekFields = WeekFields.of(Locale.getDefault())
            val thisWeekStart = today.with(weekFields.dayOfWeek(), 1)
            val weeklyDays = if (startDate.isAfter(thisWeekStart)) {
                ChronoUnit.DAYS.between(startDate, today)
            } else {
                ChronoUnit.DAYS.between(thisWeekStart, today.plusDays(1))
            }

            // 이번 달 금주 일수
            val thisMonthStart = today.withDayOfMonth(1)
            val monthlyDays = if (startDate.isAfter(thisMonthStart)) {
                ChronoUnit.DAYS.between(startDate, today)
            } else {
                ChronoUnit.DAYS.between(thisMonthStart, today.plusDays(1))
            }

            // UI 업데이트
            tvWeeklyCount.text = "${weeklyDays}일"
            tvMonthlyCount.text = "${monthlyDays}일"
            tvTotalCount.text = "${totalDays}일"

            // 요약 메시지
            val summaryMessage = when {
                totalDays >= 365 -> "1년 이상 금주를 실천하셨네요! 정말 대단합니다! 🎉"
                totalDays >= 180 -> "6개월 이상 금주를 지속하고 계시네요! 👏"
                totalDays >= 90 -> "3개월 동안 꾸준히 실천하셨어요! 💪"
                totalDays >= 30 -> "한 달 동안 잘 해내고 계세요! ⭐"
                else -> "금주를 시작한지 ${totalDays}일이 지났습니다."
            }
            tvSummary.text = summaryMessage
        }
    }
}
