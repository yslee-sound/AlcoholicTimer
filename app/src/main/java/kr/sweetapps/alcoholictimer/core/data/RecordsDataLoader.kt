package kr.sweetapps.alcoholictimer.core.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord

object RecordsDataLoader {
    private const val TAG = "RecordsDataLoader"

    fun loadSobrietyRecords(context: Context): List<SobrietyRecord> = try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val records = SobrietyRecord.fromJsonArray(recordsJson)
        records.sortedByDescending { it.endTime }
    } catch (e: Exception) {
        Log.e(TAG, "기록 로딩 중 오류 발생", e)
        emptyList()
    }

    fun clearAllRecords(context: Context): Boolean = try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // 삭제 전 로깅
        val beforeJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        Log.d(TAG, "삭제 전 기록: $beforeJson")

        // commit()을 사용하여 동기적으로 저장 (apply()는 비동기)
        val success = sharedPref.edit().putString("sobriety_records", "[]").commit()

        if (success) {
            // 삭제 후 확인
            val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
            Log.d(TAG, "삭제 후 기록: $afterJson")
            Log.d(TAG, "모든 기록 삭제 성공")
        } else {
            Log.e(TAG, "SharedPreferences commit 실패")
        }

        success
    } catch (e: Exception) {
        Log.e(TAG, "모든 기록 삭제 중 오류", e)
        false
    }
}

