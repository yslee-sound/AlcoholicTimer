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

        // [FIX] 완료된 기록만 삭제, 진행 중인 타이머는 유지 (2026-01-02)
        sharedPref.edit()
            .putString("sobriety_records", "[]")
            // start_time과 timer_completed는 건드리지 않음 (진행 중인 타이머 유지)
            .apply()

        // Verify after deletion
        val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val afterStartTime = sharedPref.getLong("start_time", 0L)
        Log.d(TAG, "Records after deletion: $afterJson")
        Log.d(TAG, "Start time after deletion: $afterStartTime (should be preserved)")
        Log.d(TAG, "All completed records deleted successfully (timer preserved)")

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

