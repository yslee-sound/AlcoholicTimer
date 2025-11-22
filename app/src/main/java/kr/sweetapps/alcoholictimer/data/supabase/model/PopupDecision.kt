package kr.sweetapps.alcoholictimer.data.supabase.model

sealed class PopupDecision {
    object None : PopupDecision()
    data class ShowEmergency(val policy: EmergencyPolicy) : PopupDecision()
    data class ShowUpdate(val policy: UpdatePolicy) : PopupDecision()
    data class ShowNotice(val announcement: Announcement) : PopupDecision()
}
