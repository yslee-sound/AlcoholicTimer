package kr.sweetapps.alcoholictimer.data.supabase.model

/** Minimal AdPolicy model mapping Supabase ad_policy fields used by AdController. */
data class AdPolicy(
    val id: Long = 0L,
    val appId: String = "",
    val isActive: Boolean = false,
    val adAppOpenEnabled: Boolean = true,
    val adInterstitialEnabled: Boolean = false,
    val adBannerEnabled: Boolean = false,
    val appOpenMaxPerHour: Int = 2,
    val appOpenMaxPerDay: Int = 15,
    val adInterstitialMaxPerHour: Int = 3,
    val adInterstitialMaxPerDay: Int = 20,
    // cooldown between app-open ads in seconds (remote-controlled)
    val appOpenCooldownSeconds: Int = 60
) {
    companion object {
        // Default fallback policy used when remote policy cannot be fetched or parsed.
        // Values chosen to preserve revenue while being conservative about frequency.
        val DEFAULT_FALLBACK = AdPolicy(
            id = 0L,
            appId = "",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 1,
            appOpenMaxPerDay = 15,
            adInterstitialMaxPerHour = 1,
            adInterstitialMaxPerDay = 5,
            appOpenCooldownSeconds = 300
        )
    }
}
