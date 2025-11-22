// Test-only models for popup policy unit tests
package kr.sweetapps.alcoholictimer.popup

// EmergencyPolicy test config
data class EmergencyPolicyConfig(
    val app_id: String,
    val is_active: Boolean,
    val content: String,
    val redirect_url: String?,
    val button_text: String?,
    val is_dismissible: Boolean
)

// NoticePolicy test config
data class NoticePolicyConfig(
    val app_id: String,
    val is_active: Boolean,
    val title: String?,
    val content: String,
    val notice_version: Int
)

// UpdatePolicy test config
data class UpdatePolicyConfig(
    val app_id: String,
    val is_active: Boolean,
    val target_version_code: Int,
    val is_force_update: Boolean,
    val release_notes: String?,
    val download_url: String?,
    val reshow_interval_hours: Long,
    val max_later_count: Int
)

// Result sealed class returned by PopupManager.getPopupToShow()
sealed class PopupResult {
    object None : PopupResult()
    data class Emergency(val policy: EmergencyPolicyConfig) : PopupResult()
    data class Update(val policy: UpdatePolicyConfig) : PopupResult()
    data class Notice(val policy: NoticePolicyConfig) : PopupResult()
}

