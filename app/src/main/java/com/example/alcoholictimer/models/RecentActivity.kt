package com.example.alcoholictimer.models

data class RecentActivity(
    val startDate: String,      // 시작일
    val endDate: String,        // 종료일
    val duration: Int,          // 지속 일수
    val isCompleted: Boolean    // 성공 여부
)
