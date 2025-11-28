package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateStats(
    val currentLaterCount: Int = 0,
    val lastDismissedAtMs: Long = 0L
)
