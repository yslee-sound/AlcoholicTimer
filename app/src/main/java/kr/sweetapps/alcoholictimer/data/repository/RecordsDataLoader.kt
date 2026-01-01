package kr.sweetapps.alcoholictimer.data.repository

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
        Log.e(TAG, "Error loading records", e)
        emptyList()
    }

    // [NEW] 캐시 무효화를 위한 콜백 리스너들
    private val clearRecordsListeners = mutableListOf<() -> Unit>()

    fun registerClearRecordsListener(listener: () -> Unit) {
        clearRecordsListeners.add(listener)
        Log.d(TAG, "Clear records listener registered, total=${clearRecordsListeners.size}")
    }

    fun unregisterClearRecordsListener(listener: () -> Unit) {
        clearRecordsListeners.remove(listener)
        Log.d(TAG, "Clear records listener unregistered, total=${clearRecordsListeners.size}")
    }

    fun clearAllRecords(context: Context): Boolean = try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // Log before deletion
        val beforeJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val beforeStartTime = sharedPref.getLong("start_time", 0L)
        Log.d(TAG, "Records before deletion: $beforeJson")
        Log.d(TAG, "Start time before deletion: $beforeStartTime")

        // [FIX] 기록 삭제 + 현재 타이머 상태 초기화 (apply로 비동기 처리하여 ANR 방지)
        sharedPref.edit()
            .putString("sobriety_records", "[]")
            .putLong("start_time", 0L)  // [NEW] 현재 타이머 시작 시간 초기화
            .putBoolean("timer_completed", false)  // [NEW] 타이머 완료 상태 초기화
            .apply()

        // Verify after deletion
        val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val afterStartTime = sharedPref.getLong("start_time", 0L)
        Log.d(TAG, "Records after deletion: $afterJson")
        Log.d(TAG, "Start time after deletion: $afterStartTime")
        Log.d(TAG, "All records and timer state deleted successfully (async)")

        // [NEW] 모든 리스너에게 캐시 무효화 알림
        clearRecordsListeners.forEach { listener ->
            try {
                listener()
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying clear records listener", e)
            }
        }

        true // apply()는 항상 성공으로 처리
    } catch (e: Exception) {
        Log.e(TAG, "Error clearing all records", e)
        false
    }
}

