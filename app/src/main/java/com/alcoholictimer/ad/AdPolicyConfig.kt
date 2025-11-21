package com.alcoholictimer.ad

// ad_policy 테이블 스키마 기반의 Kotlin Data Class
data class AdPolicyConfig(
    val id: Long = 0,
    val app_id: String = "alcoholicTimer",
    val is_active: Boolean = true,
    val ad_app_open_enabled: Boolean = true,
    val ad_interstitial_enabled: Boolean = true,
    val ad_banner_enabled: Boolean = true,
    val ad_interstitial_max_per_hour: Int = 3,
    val ad_interstitial_max_per_day: Int = 20,
    val app_open_max_per_hour: Int = 2,
    val app_open_max_per_day: Int = 15,
    val app_open_cooldown_seconds: Int = 60 // NEW: 쿨다운 시간(초)
)

