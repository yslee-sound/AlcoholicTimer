package kr.sweetapps.alcoholictimer.data.supabase.model

sealed class PopupDecision {
    object None : PopupDecision()
    data class ShowUpdate(val policy: UpdatePolicy) : PopupDecision()
}

