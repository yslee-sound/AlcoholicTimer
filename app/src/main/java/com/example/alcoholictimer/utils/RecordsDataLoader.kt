package com.example.alcoholictimer.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit

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

    fun addTestRecord(context: Context): Boolean {
        return try {
            // 현재 기록들을 먼저 로드
            val currentRecords = loadSobrietyRecords(context).toMutableList()

            // 랜덤한 테스트 기록 생성
            val random = kotlin.random.Random
            val targetDays = listOf(7, 14, 21, 30).random()
            val actualDays = random.nextInt(1, targetDays + 1)
            val isCompleted = actualDays >= targetDays

            // 랜덤한 과거 날짜 생성 (최근 30일 이내)
            val daysAgo = random.nextInt(1, 30)
            val endTime = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
            val startTime = endTime - (actualDays * 24 * 60 * 60 * 1000L)

            val testRecord = SobrietyRecord(
                id = "test_${System.currentTimeMillis()}",
                startTime = startTime,
                endTime = endTime,
                targetDays = targetDays,
                actualDays = actualDays,
                isCompleted = isCompleted,
                status = if (isCompleted) "성공" else "실패",
                createdAt = System.currentTimeMillis()
            )

            // 새 기록을 목록에 추가
            currentRecords.add(testRecord)

            // 저장
            val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)

            sharedPref.edit { putString("sobriety_records", jsonString) }

            Log.d(TAG, "테스트 기록 추가 완료: ${testRecord.id}")
            Log.d(TAG, "목표: ${targetDays}일, 달성: ${actualDays}일, 완료: ${isCompleted}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "테스트 기록 추가 중 오류 발생", e)
            false
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
