package com.example.alcoholictimer.model

data class RecentActivity(
    val endDate: String,        // 종료일
    val title: String,          // 금주/챌린지 이름
    val duration: Int,          // 지속 일수
    val hours: Int,             // 지속 시간
    val isSuccess: Boolean,     // 성공 여부
    val savedMoney: Int         // 절약 금액(만원)
)
