package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 업데이트 정책 데이터 모델
 */
@Serializable
data class UpdatePolicy(
    @SerialName("id")
    val id: String,

    @SerialName("is_active")
    val isActive: Boolean,

    @SerialName("version")
    val version: String,

    @SerialName("version_code")
    val versionCode: Int,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String,

    @SerialName("update_button_text")
    val updateButtonText: String,

    @SerialName("later_button_text")
    val laterButtonText: String? = null,

    @SerialName("features")
    val features: List<String> = emptyList(),

    @SerialName("is_force_update")
    val isForceUpdate: Boolean = false,

    @SerialName("target_version_min")
    val targetVersionMin: String? = null,

    @SerialName("target_version_max")
    val targetVersionMax: String? = null,

    @SerialName("store_url")
    val storeUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)

