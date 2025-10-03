package com.example.alcoholictimer.core.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.alcoholictimer.core.model.SobrietyRecord

object RecordsDataLoader {
    private const val TAG = "RecordsDataLoader"

    fun loadSobrietyRecords(context: Context): List<SobrietyRecord> {
        return try {
            val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"

            Log.d(TAG, "========== 디버깅 정보 ==========")
            Log.d(TAG, "저장된 JSON 문자열: $recordsJson")
            Log.d(TAG, "JSON 길이: ${recordsJson.length}")

            // SobrietyRecord의 companion object 메서드 사용
            val records = SobrietyRecord.fromJsonArray(recordsJson)

            Log.d(TAG, "파싱된 기록 개수: ${records.size}")
            records.forEachIndexed { index, record ->
                Log.d(TAG, "기록 $index:")
                Log.d(TAG, "  ID: ${record.id}")
                Log.d(TAG, "  시작시간: ${record.startTime}")
                Log.d(TAG, "  종료시간: ${record.endTime}")
                Log.d(TAG, "  목표일수: ${record.targetDays}")
                Log.d(TAG, "  달성일수: ${record.actualDays}")
                Log.d(TAG, "  완료여부: ${record.isCompleted}")
                Log.d(TAG, "  상태: ${record.status}")
                Log.d(TAG, "  생성시간: ${record.createdAt}")
            }
            Log.d(TAG, "===============================")

            // 종료일 기준으로 최신 순 정렬 (가장 최근 종료된 기록이 맨 위)
            records.sortedByDescending { it.endTime }
        } catch (e: Exception) {
            Log.e(TAG, "기록 로딩 중 오류 발생", e)
            Log.e(TAG, "오류 상세: ${e.message}")
            Log.e(TAG, "스택 트레이스: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    // 모든 금주 기록 삭제: SharedPreferences의 "sobriety_records"를 빈 배열로 초기화
    fun clearAllRecords(context: Context): Boolean {
        return try {
            val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            // 안전하게 빈 배열로 덮어쓰기 (다른 설정값 보존)
            sharedPref.edit { putString("sobriety_records", "[]") }
            Log.d(TAG, "모든 기록 삭제 완료: sobriety_records=[]")
            true
        } catch (e: Exception) {
            Log.e(TAG, "모든 기록 삭제 중 오류", e)
            false
        }
    }
}
