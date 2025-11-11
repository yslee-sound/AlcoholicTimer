package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 긴급 공지 정책 데이터 모델
 */
@Serializable
data class EmergencyPolicy(
    @SerialName("id")
    val id: String,

    @SerialName("is_active")
    val isActive: Boolean,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String,

    @SerialName("new_app_name")
    val newAppName: String? = null,

    @SerialName("new_app_package")
    val newAppPackage: String? = null,

    @SerialName("button_text")
    val buttonText: String,

    @SerialName("support_url")
    val supportUrl: String? = null,

    @SerialName("support_button_text")
    val supportButtonText: String? = null,

    @SerialName("can_migrate_data")
    val canMigrateData: Boolean = false,

    @SerialName("is_dismissible")
    val isDismissible: Boolean = false,

    @SerialName("badge_text")
    val badgeText: String? = null,

    @SerialName("migration_message")
    val migrationMessage: String? = null,

    @SerialName("priority")
    val priority: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null
)

