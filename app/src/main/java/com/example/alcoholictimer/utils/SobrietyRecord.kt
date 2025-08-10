package com.example.alcoholictimer.utils

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 금주 기록을 저장하는 데이터 클래스
 *
 * @property id 고유 ID (저장 시간 기반, String 타입)
 * @property startTime 금주 시작 시간 (밀리초 타임스탬프)
 * @property endTime 금주 종료 시간 (밀리초 타임스탬프)
 * @property targetDays 목표 기간 (일 단위)
 * @property actualDays 실제 달성한 기간 (일 단위)
 * @property isCompleted 목표를 완료했는지 여부 (true: 완료, false: 중도 포기)
 * @property status 상태 ("완료" 또는 "중지")
 * @property createdAt 기록 생성 시간 (밀리초 타임스탬프)
 */
data class SobrietyRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val targetDays: Int,
    val actualDays: Int,
    val isCompleted: Boolean,
    val status: String,
    val createdAt: Long
) {
    /**
     * 달성률 계산 (백분율)
     */
    val achievedPercentage: Int
        get() = if (targetDays > 0) {
            ((actualDays.toFloat() / targetDays.toFloat()) * 100).toInt()
        } else {
            0
        }

    /**
     * 시작 날짜를 문자열로 포맷
     */
    val startDateString: String
        get() = formatDate(startTime)

    /**
     * 종료 날짜를 문자열로 포맷
     */
    val endDateString: String
        get() = formatDate(endTime)

    /**
     * 달성한 레벨 계산
     */
    val achievedLevel: Int
        get() = when {
            actualDays < 7 -> 1
            actualDays < 30 -> 2
            actualDays < 90 -> 3
            actualDays < 365 -> 4
            else -> 5
        }

    /**
     * 달성한 레벨 타이틀
     */
    val levelTitle: String
        get() = when {
            actualDays < 7 -> "시작"
            actualDays < 30 -> "작심 7일"
            actualDays < 90 -> "한 달 클리어"
            actualDays < 365 -> "3개월 클리어"
            else -> "절제의 레전드"
        }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }

    /**
     * 기록을 JSONObject로 변환
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("startTime", startTime)
        json.put("endTime", endTime)
        json.put("targetDays", targetDays)
        json.put("actualDays", actualDays)
        json.put("isCompleted", isCompleted)
        json.put("status", status)
        json.put("createdAt", createdAt)
        return json
    }

    companion object {
        /**
         * JSONObject에서 기록 객체 생성
         */
        fun fromJson(json: JSONObject): SobrietyRecord {
            return SobrietyRecord(
                id = json.getString("id"),
                startTime = json.getLong("startTime"),
                endTime = json.getLong("endTime"),
                targetDays = json.getInt("targetDays"),
                actualDays = json.getInt("actualDays"),
                isCompleted = json.getBoolean("isCompleted"),
                status = json.getString("status"),
                createdAt = json.getLong("createdAt")
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
