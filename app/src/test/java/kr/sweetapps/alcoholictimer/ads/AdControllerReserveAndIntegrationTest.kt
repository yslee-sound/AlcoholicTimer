package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import kr.sweetapps.alcoholictimer.ui.ad.AdController
import kr.sweetapps.alcoholictimer.ui.ad.InterstitialAdManager
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AdControllerReserveAndIntegrationTest {

    @Test
    fun reserve_unreserve_behavior() {
        val policy = AdPolicy(
            id = 10L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 10,
            appOpenMaxPerDay = 100,
            adInterstitialMaxPerHour = 1,
            adInterstitialMaxPerDay = 5,
            appOpenCooldownSeconds = 1
        )
        AdController.setPolicyForTest(policy)

        val first = AdController.reserveInterstitialSlot()
        assertTrue("First reservation should succeed", first)

        val second = AdController.reserveInterstitialSlot()
        assertFalse("Second reservation should be denied due to per-hour limit", second)

        // Unreserve and reserve again
        AdController.unreserveInterstitialSlot()
        val third = AdController.reserveInterstitialSlot()
        assertTrue("After unreserve, reservation should succeed again", third)

        // cleanup
        try { AdController.unreserveInterstitialSlot() } catch (_: Throwable) {}
    }

    @Test
    fun window_expiry_resets_counters() {
        val policy = AdPolicy(
            id = 11L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 100,
            appOpenMaxPerDay = 1000,
            adInterstitialMaxPerHour = 100,
            adInterstitialMaxPerDay = 1000,
            appOpenCooldownSeconds = 1
        )
        AdController.setPolicyForTest(policy)

        val cls = AdController::class.java
        // set shownInterstitial counters to non-zero
        val shownHourField = cls.getDeclaredField("shownInterstitialThisHour").apply { isAccessible = true }
        val shownDayField = cls.getDeclaredField("shownInterstitialToday").apply { isAccessible = true }
        val shownHour = shownHourField.get(null) as java.util.concurrent.atomic.AtomicInteger
        val shownDay = shownDayField.get(null) as java.util.concurrent.atomic.AtomicInteger
        shownHour.set(5)
        shownDay.set(10)

        // set window starts to the past so they will be considered expired
        val hourWindowField = cls.getDeclaredField("hourWindowStart").apply { isAccessible = true }
        val dayWindowField = cls.getDeclaredField("dayWindowStart").apply { isAccessible = true }
        val now = System.currentTimeMillis()
        hourWindowField.setLong(null, now - 2 * 60 * 60 * 1000L) // 2 hours ago
        dayWindowField.setLong(null, now - 25 * 60 * 60 * 1000L) // 25 hours ago

        // Trigger evaluation which should call resetWindowsIfNeeded()
        val allowed = AdController.canShowInterstitialNow()
        // After reset, counters should be zero
        assertEquals(0, shownHour.get())
        assertEquals(0, shownDay.get())
        assertTrue("With permissive policy after reset, interstitial should be allowed", allowed)
    }

    @Test
    fun interstitial_blocked_when_fullscreen() {
        val policy = AdPolicy(
            id = 12L,
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

        try {
            AdController.setFullScreenAdShowing(true)
            val activity = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
            val willShow = InterstitialAdManager.maybeShowIfEligible(activity)
            assertFalse("InterstitialAdManager should not show when full-screen ad is active", willShow)
        } finally {
            AdController.setFullScreenAdShowing(false)
        }
    }
}

