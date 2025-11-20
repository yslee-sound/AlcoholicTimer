package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal UpdatePolicy stub. */
@Serializable
data class UpdatePolicy(
    @SerialName("id") val id: Long = 0,
    @SerialName("app_id") val appId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("target_version_code") val targetVersionCode: Long = 0L,
    @SerialName("is_force_update") val isForceUpdate: Boolean = false,
    @SerialName("max_later_count") val maxLaterCount: Int = 0,
    // fields used by OptionalUpdateDialog usage sites
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)
