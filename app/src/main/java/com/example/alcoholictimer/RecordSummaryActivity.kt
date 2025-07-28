package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

            // UI 요소 초기화
            val tvRecordScreenTitle = view.findViewById<TextView>(R.id.tvRecordScreenTitle)
            val tvSummaryTitle = view.findViewById<TextView>(R.id.tvSummaryTitle)
            val tvPeriod = view.findViewById<TextView>(R.id.tvPeriod)
            val btnNewChallenge = view.findViewById<Button>(R.id.btnNewChallenge)

            if (tvRecordScreenTitle == null || tvSummaryTitle == null || tvPeriod == null || btnNewChallenge == null) {
                Log.e(TAG, "One or more UI elements not found in layout")
                return
            }

            // 기본 타이틀은 항상 표시
            tvRecordScreenTitle.text = "기록화면"

            // 레코드가 있는 경우에만 데이터 표시
            if (record != null) {
                // 제목 설정
                if (record!!.isCompleted) {
                    tvSummaryTitle.text = "목표 달성을 축하합니다!"
                } else {
                    tvSummaryTitle.text = "금주 기록 요약"
                }

                // 날짜 포맷 설정
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())
                val startDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(record!!.startDate) ?: Date())
                val endDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(record!!.endDate) ?: Date())

                // 기간 표시
                tvPeriod.text = "시작: $startDate\n종료: $endDate"
            } else {
                // 레코드가 없는 경우 기본 텍스트 설정
                tvSummaryTitle.text = "금주 기록"
                tvPeriod.text = "기록 정보 없음"
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
