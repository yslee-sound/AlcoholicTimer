package kr.sweetapps.alcoholictimer.feature.detail

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.ui.screens.DetailScreen as UiDetailScreen

@Composable
fun DetailScreen(
    startTime: Long,
    endTime: Long,
    targetDays: Float,
    actualDays: Int,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onDeleted: (() -> Unit)? = null
) {
    // Provide the original deleteRecord implementation via onDelete callback so
    // callers don't need to change. This keeps behavior identical to original file.
    val context = LocalContext.current
    val deleteImpl: (Long, Long) -> Unit = deleteImpl@{ s, e ->
        Log.d("DetailScreen", "deleteImpl called for start=$s end=$e")
        val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("sobriety_records", null) ?: run {
            Log.d("DetailScreen", "no sobriety_records found in sharedPref")
            return@deleteImpl
        }
        try {
            Log.d("DetailScreen", "currentRecordsJson=${jsonString}")
            val originalArray = org.json.JSONArray(jsonString)
            val newArray = org.json.JSONArray()
            var removed = 0
            for (i in 0 until originalArray.length()) {
                val obj = originalArray.getJSONObject(i)
                val sv = obj.optLong("startTime", obj.optLong("start_time", -1))
                val ev = obj.optLong("endTime", obj.optLong("end_time", -1))
                if (sv == s && ev == e) {
                    removed++
                    Log.d("DetailScreen", "matched and removing index=$i sv=$sv ev=$ev")
                } else {
                    newArray.put(obj)
                }
            }
            if (removed > 0) {
                val committed = sharedPref.edit().putString("sobriety_records", newArray.toString()).commit()
                Log.d("DetailScreen", "removed=$removed committed=$committed remainingLen=${newArray.length()}")
                if (!committed) {
                    Log.e("DetailScreen", "SharedPreferences.commit() failed")
                    Toast.makeText(context, "기록 삭제 실패(저장 오류)", Toast.LENGTH_SHORT).show()
                } else {
                    // notify caller that a deletion occurred (so they can refresh lists/stats)
                    try { onDeleted?.invoke() } catch (_: Exception) {}
                    Toast.makeText(context, "기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                }
                // read back and log
                val afterJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
                Log.d("DetailScreen", "afterRecordsJson=${afterJson}")
            } else {
                Log.d("DetailScreen", "no matching record removed (removed=0)")
                Toast.makeText(context, "삭제할 기록을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            Log.e("DetailScreen", "기록 삭제 중 오류", ex)
        }
    }

    UiDetailScreen(startTime, endTime, targetDays, actualDays, isCompleted, onBack, onDelete = deleteImpl)
}
