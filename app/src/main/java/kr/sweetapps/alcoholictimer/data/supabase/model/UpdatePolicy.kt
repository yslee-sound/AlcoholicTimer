package kr.sweetapps.alcoholictimer.data.supabase.model

/**
 * Minimal update policy model matching Supabase schema used by the app.
 */

data class UpdatePolicy(
    val id: Long = 0L,
    val appId: String = "",
    val targetVersionCode: Int = 0,
    val isForceUpdate: Boolean = false,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)

