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

    fun clearAllRecords(context: Context): Boolean = try {
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // Log before deletion
        val beforeJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        Log.d(TAG, "Records before deletion: $beforeJson")

        // Use commit() for synchronous save (apply() is asynchronous)
        val success = sharedPref.edit().putString("sobriety_records", "[]").commit()

        if (success) {
            // Verify after deletion
            val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
            Log.d(TAG, "Records after deletion: $afterJson")
            Log.d(TAG, "All records deleted successfully")
        } else {
            Log.e(TAG, "SharedPreferences commit failed")
        }

        success
    } catch (e: Exception) {
        Log.e(TAG, "Error clearing all records", e)
        false
    }
}

