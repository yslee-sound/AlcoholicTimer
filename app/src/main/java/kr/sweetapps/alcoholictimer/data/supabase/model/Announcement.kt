package kr.sweetapps.alcoholictimer.data.supabase.model

/** Minimal Announcement stub. */
data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val isActive: Boolean = false
)
