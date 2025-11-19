package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * UpdatePolicy matches Supabase `update_policy` table schema.
 */
@Serializable
data class UpdatePolicy(
    @SerialName("id")
    val id: Long,

    @SerialName("app_id")
    val appId: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    // target version the policy applies to (integer)
    @SerialName("target_version_code")
    val targetVersionCode: Long = 0L,

    @SerialName("is_force_update")
    val isForceUpdate: Boolean = false,

    @SerialName("release_notes")
    val releaseNotes: String? = null,

    @SerialName("download_url")
    val downloadUrl: String? = null,

    @SerialName("reshow_interval_hours")
    val reshowIntervalHours: Int? = null,

    @SerialName("reshow_interval_minutes")
    val reshowIntervalMinutes: Int? = null,

    @SerialName("reshow_interval_seconds")
    val reshowIntervalSeconds: Int? = null,

    @SerialName("max_later_count")
    val maxLaterCount: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null
)
