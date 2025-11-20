package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.Serializable

/** PopupDecision sealed type used by PopupPolicyManager.decidePopup.
 *  Keep a lightweight sealed hierarchy to match calling code.
 */
@Serializable
sealed class PopupDecision {
    @Serializable
    object Noop : PopupDecision()

    @Serializable
    data class ShowUpdate(val policy: UpdatePolicy) : PopupDecision()
}
