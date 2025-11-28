package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdControllerUnitTest {

    @Test
    fun test_setPolicyForTest_allowsInterstitial_when_enabled() {
        // permissive policy: interstitials enabled with generous limits
        val policy = AdPolicy(
            id = 1L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 10,
            appOpenMaxPerDay = 100,
            adInterstitialMaxPerHour = 10,
            adInterstitialMaxPerDay = 100,
            appOpenCooldownSeconds = 1
        )
        AdController.setPolicyForTest(policy)

        val canShow = AdController.canShowInterstitialNow()
        assertTrue("With permissive policy, interstitial should be allowed", canShow)
    }

    @Test
    fun test_setPolicyForTest_blocksInterstitial_when_disabled() {
        val policy = AdPolicy(
            id = 2L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = false, // disabled
            adBannerEnabled = true,
            appOpenMaxPerHour = 10,
            appOpenMaxPerDay = 100,
            adInterstitialMaxPerHour = 0,
            adInterstitialMaxPerDay = 0,
            appOpenCooldownSeconds = 1
        )
        AdController.setPolicyForTest(policy)
        val canShow = AdController.canShowInterstitialNow()
        assertFalse("When adInterstitialEnabled=false, interstitial should be denied", canShow)
    }

    @Test
    fun test_appOpenCooldown_respected() {
        val policy = AdPolicy(
            id = 3L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 10,
            appOpenMaxPerDay = 100,
            adInterstitialMaxPerHour = 10,
            adInterstitialMaxPerDay = 100,
            appOpenCooldownSeconds = 2 // 2 seconds cooldown
        )
        AdController.setPolicyForTest(policy)
        // first show
        AdController.recordAppOpenShown(mockContext())
        // immediately should be in cooldown
        val inCd = AdController.isAppOpenInCooldown()
        assertTrue("App-open should be in cooldown right after recording show", inCd)
    }

    // Helper: minimal mock Context used by recordAppOpenShown (only used for set/get shared prefs in some flows)
    private fun mockContext(): android.content.Context {
        // Use Robolectric to create a lightweight Activity/context for unit tests
        return Robolectric.buildActivity(android.app.Activity::class.java).create().get().applicationContext
    }
}
