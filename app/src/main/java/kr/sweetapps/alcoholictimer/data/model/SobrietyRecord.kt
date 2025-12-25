package kr.sweetapps.alcoholictimer.data.model

import kr.sweetapps.alcoholictimer.util.utils.PercentUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class SobrietyRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val targetDays: Int,
    val actualDays: Double,  // [REFACTOR] Int → Double로 변경하여 소수점 정확도 유지
    val isCompleted: Boolean,
    val status: String,
    val createdAt: Long,
    val percentage: Int? = null,
    val memo: String? = null
) {
    val achievedPercentage: Int
        get() = percentage ?: if (targetDays > 0) {
            PercentUtils.roundPercent((actualDays / targetDays.toDouble()) * 100.0)
        } else 0

    val startDateString: String get() = formatDate(startTime)
    val endDateString: String get() = formatDate(endTime)

    // [REFACTOR] 레벨 판정 시 반올림 적용 (0.6일 → 1일로 처리)
    val achievedLevel: Int
        get() {
            val roundedDays = kotlin.math.round(actualDays).toInt()
            return when {
                roundedDays < 7 -> 1
                roundedDays < 30 -> 2
                roundedDays < 90 -> 3
                roundedDays < 365 -> 4
                else -> 5
            }
        }

    val levelTitle: String
        get() {
            val roundedDays = kotlin.math.round(actualDays).toInt()
            return when {
                roundedDays < 7 -> "시작"
                roundedDays < 30 -> "작심 7일"
                roundedDays < 90 -> "한 달 클리어"
                roundedDays < 365 -> "3개월 클리어"
                else -> "절제의 레전드"
            }
        }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startTime", startTime)
        put("endTime", endTime)
        put("targetDays", targetDays)
        put("actualDays", actualDays)
        put("isCompleted", isCompleted)
        put("status", status)
        put("createdAt", createdAt)
        percentage?.let { put("percentage", it) }
        memo?.let { put("memo", it) }
    }

    companion object {
        /**
         * [ROBUST] JSON에서 SobrietyRecord 생성 - 안전한 파싱 로직
         * - 알 수 없는 필드: 자동으로 무시됨 (JSONObject는 기본적으로 unknown keys를 무시)
         * - 없는 필드: optXXX() 메서드와 기본값으로 안전하게 처리
         * - 파싱 실패: null 반환하여 호출자가 처리하도록 함
         */
        fun fromJson(json: JSONObject): SobrietyRecord? {
            return try {
                SobrietyRecord(
                    // 필수 필드: 없으면 예외 발생 → try-catch로 포착
                    id = json.optString("id", UUID.randomUUID().toString()),
                    startTime = json.optLong("startTime", 0L),
                    endTime = json.optLong("endTime", 0L),
                    targetDays = json.optInt("targetDays", 1), // [CHANGED] 기본값 21 -> 1 (2025-12-25)

                    // [ROBUST] actualDays: Int/Double 호환 처리
                    actualDays = when {
                        json.has("actualDays") && !json.isNull("actualDays") -> {
                            try {
                                json.getDouble("actualDays")
                            } catch (e: Exception) {
                                // 구버전이 Int로 저장했을 경우 대비
                                json.optInt("actualDays", 0).toDouble()
                            }
                        }
                        else -> 0.0
                    },

                    isCompleted = json.optBoolean("isCompleted", false),
                    status = json.optString("status", "unknown"),
                    createdAt = json.optLong("createdAt", System.currentTimeMillis()),

                    // 선택 필드: 없으면 null
                    percentage = if (json.has("percentage") && !json.isNull("percentage")) {
                        json.optInt("percentage", 0)
                    } else null,
                    memo = if (json.has("memo") && !json.isNull("memo")) {
                        json.optString("memo", "")
                    } else null
                )
            } catch (e: Exception) {
                android.util.Log.e("SobrietyRecord", "Failed to parse JSON: ${e.message}")
                null
            }
        }

        fun toJsonArray(records: List<SobrietyRecord>): String {
            val jsonArray = JSONArray()
            records.forEach { record -> jsonArray.put(record.toJson()) }
            return jsonArray.toString()
        }

        /**
         * [ROBUST] JSON 배열에서 리스트 생성 - 안전한 파싱
         * - 파싱 실패한 개별 아이템은 건너뛰고 성공한 것만 반환
         * - 전체 파싱 실패 시 빈 리스트 반환
         */
        fun fromJsonArray(jsonString: String): List<SobrietyRecord> {
            val records = mutableListOf<SobrietyRecord>()
            try {
                if (jsonString.isBlank()) return emptyList()

                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    try {
                        val jsonObject = jsonArray.getJSONObject(i)
                        fromJson(jsonObject)?.let { records.add(it) }
                    } catch (e: Exception) {
                        // 개별 아이템 파싱 실패는 건너뛰기
                        android.util.Log.w("SobrietyRecord", "Failed to parse item $i: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                // 전체 파싱 실패
                android.util.Log.e("SobrietyRecord", "Failed to parse JSON array: ${e.message}")
            }
            return records
        }
    }
}

