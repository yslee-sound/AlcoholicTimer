package com.example.alcoholictimer.utils

import org.json.JSONArray
import org.json.JSONObject

/**
 * 금주 기록을 저장하는 데이터 클래스
 *
 * @property id 고유 ID (저장 시간 기반)
 * @property startDate 금주 시작 날짜 (문자열 형식: "yyyy-MM-dd HH:mm:ss")
 * @property endDate 금주 종료 날짜 (문자열 형식: "yyyy-MM-dd HH:mm:ss")
 * @property duration 목표 기간 (일 또는 분 단위)
 * @property achievedDays 실제 달성한 기간 (일 또는 분 단위, 완료 시 duration과 동일)
 * @property achievedLevel 달성한 레벨
 * @property levelTitle 달성한 레벨 타이틀
 * @property isCompleted 목표를 완료했는지 여부 (true: 완료, false: 중도 포기)
 */
data class SobrietyRecord(
    val id: Long,
    val startDate: String,
    val endDate: String,
    val duration: Int,
    val achievedDays: Int = duration, // 완료된 경우 기본값은 전체 기간
    val achievedLevel: Int,
    val levelTitle: String,
    val isCompleted: Boolean
) {
    /**
     * 달성률 계산 (백분율)
     */
    val achievedPercentage: Int
        get() = if (duration > 0) {
            ((achievedDays.toFloat() / duration.toFloat()) * 100).toInt()
        } else {
            0
        }

    /**
     * 기록을 JSONObject로 변환
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("startDate", startDate)
        json.put("endDate", endDate)
        json.put("duration", duration)
        json.put("achievedDays", achievedDays)
        json.put("achievedLevel", achievedLevel)
        json.put("levelTitle", levelTitle)
        json.put("isCompleted", isCompleted)
        return json
    }

    companion object {
        /**
         * JSONObject에서 기록 객체 생성
         */
        fun fromJson(json: JSONObject): SobrietyRecord {
            return SobrietyRecord(
                id = json.getLong("id"),
                startDate = json.getString("startDate"),
                endDate = json.getString("endDate"),
                duration = json.getInt("duration"),
                achievedDays = json.getInt("achievedDays"),
                achievedLevel = json.getInt("achievedLevel"),
                levelTitle = json.getString("levelTitle"),
                isCompleted = json.getBoolean("isCompleted")
            )
        }

        /**
         * 기록 목록을 JSON 문자열로 변환
         */
        fun toJsonArray(records: List<SobrietyRecord>): String {
            val jsonArray = JSONArray()
            records.forEach { record ->
                jsonArray.put(record.toJson())
            }
            return jsonArray.toString()
        }

        /**
         * JSON 문자열에서 기록 목록 생성
         */
        fun fromJsonArray(jsonString: String): List<SobrietyRecord> {
            val records = mutableListOf<SobrietyRecord>()
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    records.add(fromJson(jsonObject))
                }
            } catch (e: Exception) {
                // JSON 파싱 오류 시 빈 목록 반환
                e.printStackTrace()
            }
            return records
        }
    }
}
