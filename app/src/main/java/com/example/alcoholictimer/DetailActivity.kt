package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {

    private lateinit var btnBack: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvRecordTitle: TextView
    private lateinit var tvMainNumber: TextView
    private lateinit var tvMainUnit: TextView
    private lateinit var tvTotalDays: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvSavedMoney: TextView
    private lateinit var tvEncouragementMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        initViews()
        setupClickListeners()
        loadRecordData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvDateTime = findViewById(R.id.tvDateTime)
        tvRecordTitle = findViewById(R.id.tvRecordTitle)
        tvMainNumber = findViewById(R.id.tvMainNumber)
        tvMainUnit = findViewById(R.id.tvMainUnit)
        tvTotalDays = findViewById(R.id.tvTotalDays)
        tvLevel = findViewById(R.id.tvLevel)
        tvSavedMoney = findViewById(R.id.tvSavedMoney)
        tvEncouragementMessage = findViewById(R.id.tvEncouragementMessage)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            // 메인 화면으로 돌아가기
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun loadRecordData() {
        val recordId = intent.getLongExtra("record_id", -1)

        if (recordId != -1L) {
            // SharedPreferences에서 저장된 모든 기록을 로드
            val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
            val recordsJson = sharedPref.getString("records", "[]")

            if (!recordsJson.isNullOrEmpty()) {
                try {
                    val records = com.example.alcoholictimer.utils.SobrietyRecord.fromJsonArray(recordsJson)
                    // recordId로 해당 기록 찾기
                    val targetRecord = records.find { it.id == recordId }

                    if (targetRecord != null) {
                        // 찾은 기록으로 화면 표시
                        displayRecordFromSobrietyRecord(targetRecord)
                    } else {
                        // 기록을 찾지 못한 경우 기본값으로 표시
                        displayDefaultRecord()
                    }
                } catch (e: Exception) {
                    // JSON 파싱 오류 시 기본값으로 표시
                    displayDefaultRecord()
                }
            } else {
                // 기록이 없는 경우 기본값으로 표시
                displayDefaultRecord()
            }
        } else {
            // recordId가 없는 경우 기본값으로 표시
            displayDefaultRecord()
        }
    }

    private fun displayRecordFromSobrietyRecord(record: com.example.alcoholictimer.utils.SobrietyRecord) {
        // 현재 날짜/시간 표시
        val currentTime = SimpleDateFormat("M월 d일 - a h:mm", Locale.KOREA)
        tvDateTime.text = currentTime.format(Date())

        // 기록 제목 생성
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
        tvRecordTitle.text = "${month}월 ${weekOfMonth}주 금주 기록"

        // 메인 숫자와 단위 설정 (항상 "일"로 표시)
        tvMainNumber.text = record.duration.toString()
        tvMainUnit.text = "일"

        // 통계 정보 설정
        tvTotalDays.text = "총 ${record.duration}일"
        tvLevel.text = "Level ${record.achievedLevel}"

        // 절약 금액 계산 (일당 2,000원)
        val savedAmount = record.duration * 2000
        tvSavedMoney.text = String.format("%,d원", savedAmount)

        // 응원 메시지 설정
        val encouragementMessage = getEncouragementMessage(record.achievedLevel, record.duration, "일")
        tvEncouragementMessage.text = "\"$encouragementMessage\""
    }

    private fun displayDefaultRecord() {
        // 기본값으로 화면 표시
        val currentTime = SimpleDateFormat("M월 d일 - a h:mm", Locale.KOREA)
        tvDateTime.text = currentTime.format(Date())

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
        tvRecordTitle.text = "${month}월 ${weekOfMonth}주 금주 기록"

        tvMainNumber.text = "7"
        tvMainUnit.text = "일"
        tvTotalDays.text = "총 7일"
        tvLevel.text = "Level 1"
        tvSavedMoney.text = "14,000원"
        tvEncouragementMessage.text = "\"첫걸음 성공! 계속 도전하세요.\""
    }

    private fun getEncouragementMessage(level: Int, targetDays: Int, unit: String): String {
        return when (level) {
            1 -> "첫걸음 성공! 계속 도전하세요."
            2 -> "${targetDays}${unit} 달성! 좋은 습관이 만들어지고 있어요."
            3 -> "정말 대단해요! ${targetDays}${unit}를 완주하셨군요!"
            4 -> "놀라운 의지력이에요! 계속해서 건강한 생활을 유지하세요."
            5 -> "정말 자랑스러워요! ${targetDays}${unit}는 쉽지 않은 도전이었을 텐데요."
            6 -> "대단한 성취입니다! 당신의 의지력에 박수를 보냅니다."
            7 -> "완벽한 도전 완수! 당신은 진정한 금주 마스터입니다! 🎉"
            else -> "훌륭한 도전이었습니다!"
        }
    }

    @Deprecated("Use onBackPressedDispatcher instead")
    override fun onBackPressed() {
        // 시스템 뒤로가기 버튼도 같은 동작
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
