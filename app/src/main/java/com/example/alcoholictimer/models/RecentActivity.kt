package com.example.alcoholictimer.models

import org.json.JSONArray
import org.json.JSONObject

data class RecentActivity(
    val id: Long = System.currentTimeMillis(),  // 고유 ID
    val startDate: String,      // 시작일 (yyyy-MM-dd)
    val endDate: String,        // 종료일 (yyyy-MM-dd)
    val title: String = "금주",  // 활동 제목
    val duration: Int,          // 지속 일수/시간/초 (테스트 모드에 따라)
    val hours: Int = 0,         // 지속 시간 (실제 경과 시간 - 시)
    val isSuccess: Boolean,     // 성공 여부
    val savedMoney: Int = 0,    // 절약 금액(만원)
    val testMode: Int = 0       // 테스트 모드 정보 저장
) {
    /**
     * RecentActivity를 JSON으로 변환
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("startDate", startDate)
        json.put("endDate", endDate)
        json.put("title", title)
        json.put("duration", duration)
        json.put("hours", hours)
        json.put("isSuccess", isSuccess)
        json.put("savedMoney", savedMoney)
        json.put("testMode", testMode)
        return json
    }

    companion object {
        /**
         * JSON에서 RecentActivity 객체로 변환
         */
        fun fromJson(json: JSONObject): RecentActivity {
            return RecentActivity(
                id = json.optLong("id", System.currentTimeMillis()),
                startDate = json.optString("startDate", ""),
                endDate = json.optString("endDate", ""),
                title = json.optString("title", "금주"),
                duration = json.optInt("duration", 0),
                hours = json.optInt("hours", 0),
                isSuccess = json.optBoolean("isSuccess", false),
                savedMoney = json.optInt("savedMoney", 0),
                testMode = json.optInt("testMode", 0)
            )
        }

        /**
         * JSON 문자열에서 RecentActivity 객체로 변환
         */
        fun fromJson(jsonString: String): RecentActivity {
            return fromJson(JSONObject(jsonString))
        }

        /**
         * JSON 배열에서 RecentActivity 리스트로 변환
         */
        fun fromJsonArray(jsonString: String): List<RecentActivity> {
            return try {
                val jsonArray = JSONArray(jsonString)
                val activities = mutableListOf<RecentActivity>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    activities.add(fromJson(jsonObject))
                }

                activities
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * RecentActivity 리스트를 JSON 배열로 변환
         */
        fun toJsonArray(activities: List<RecentActivity>): String {
            val jsonArray = JSONArray()
            activities.forEach { activity ->
                jsonArray.put(activity.toJson())
            }
            return jsonArray.toString()
        }
    }
}
