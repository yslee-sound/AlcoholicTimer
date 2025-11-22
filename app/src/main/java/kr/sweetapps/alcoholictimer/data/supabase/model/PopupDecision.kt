package kr.sweetapps.alcoholictimer.data.supabase.model

import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement

sealed class PopupDecision {
    object None : PopupDecision()
    data class ShowUpdate(val policy: UpdatePolicy) : PopupDecision()
    data class ShowNotice(val announcement: Announcement) : PopupDecision()
}
