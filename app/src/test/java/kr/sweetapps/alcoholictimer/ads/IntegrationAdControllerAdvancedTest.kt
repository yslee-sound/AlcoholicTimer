// New tests for advanced AdController behaviors
package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.os.Looper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import org.robolectric.shadows.ShadowLooper
import java.util.regex.Pattern

@RunWith(RobolectricTestRunner::class)
class IntegrationAdControllerAdvancedTest {
    @Before
    fun setup() {
        // ensure clean test policy
        val policy = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy(
            id = 1L,
            appId = "kr.sweetapps.alcoholictimer",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = true,
            appOpenMaxPerHour = 100,
            appOpenMaxPerDay = 1000,
            adInterstitialMaxPerHour = 100,
            adInterstitialMaxPerDay = 1000,
            appOpenCooldownSeconds = 2
        )
        AdController.setPolicyForTest(policy)
        // clear full-screen flag
        AdController.setFullScreenAdShowing(false)
        AdController.setInterstitialShowing(false)
    }

    private fun extractInt(snapshot: String, key: String): Int {
        val p = Pattern.compile("$key=(\\d+)")
        val m = p.matcher(snapshot)
        return if (m.find()) m.group(1)?.toInt() ?: -1 else -1
    }

    @Test
    fun reserve_and_unreserve_updates_counters() {
        val before = AdController.debugSnapshot()
        val beforeHour = extractInt(before, "interCountsHour")

        val reserved = AdController.reserveInterstitialSlot()
        assertTrue("reserve should succeed under permissive policy", reserved)

        val mid = AdController.debugSnapshot()
        val midHour = extractInt(mid, "interCountsHour")
        assertEquals("interCountsHour should increment by 1 after reserve", beforeHour + 1, midHour)

        AdController.unreserveInterstitialSlot()
        val after = AdController.debugSnapshot()
        val afterHour = extractInt(after, "interCountsHour")
        assertEquals("interCountsHour should decrement back after unreserve", beforeHour, afterHour)
    }

    @Test
    fun reserve_then_popup_unreserve_scenario() {
        // simulate reservation
        val reserved = AdController.reserveInterstitialSlot()
        assertTrue(reserved)
        val snapshotAfterReserve = AdController.debugSnapshot()
        val countAfterReserve = extractInt(snapshotAfterReserve, "interCountsHour")

        // popup appears -> full-screen flag set
        AdController.setFullScreenAdShowing(true)
        assertTrue(AdController.isFullScreenAdShowing())

        // simulate that ad failed to show due to popup and must unreserve
        AdController.unreserveInterstitialSlot()
        val snapshotAfterUnreserve = AdController.debugSnapshot()
        val countAfterUnreserve = extractInt(snapshotAfterUnreserve, "interCountsHour")

        assertEquals("unreserve should decrement interstitial counter when called after popup", countAfterReserve - 1, countAfterUnreserve)

        // clear popup
        AdController.setFullScreenAdShowing(false)
    }

    @Test
    fun appOpen_cooldown_enforced() {
        // record an app-open shown now
        val activity = ApplicationProvider.getApplicationContext<android.app.Application>()
        AdController.recordAppOpenShown(activity)

        // immediately should be in cooldown (policy cooldown = 2s)
        val blocked = !AdController.canShowAppOpen(activity)
        assertTrue("App-open should be blocked immediately after shown due to cooldown", blocked)

        // Avoid real sleep flakiness: set lastAppOpenShownAt to past (cooldown elapsed) via reflection
        try {
            val cls = AdController::class.java
            val lastField = cls.getDeclaredField("lastAppOpenShownAt").apply { isAccessible = true }
            val policyField = cls.getDeclaredField("currentPolicy").apply { isAccessible = true }
            val policy = policyField.get(null) as? kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
            val cdSec = policy?.appOpenCooldownSeconds ?: 0
            // set last shown to (now - cooldown - 100ms)
            lastField.setLong(null, System.currentTimeMillis() - (cdSec * 1000L) - 100L)
        } catch (_: Throwable) {}

        val allowed = AdController.canShowAppOpen(activity)
        assertTrue("App-open should be allowed after cooldown elapses", allowed)
    }

    @Test
    fun null_policy_blocks_ad_types() {
        // simulate policy not loaded
        AdController.setPolicyForTest(null)

        val activity = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertFalse("canShowAppOpen should be false when no policy", AdController.canShowAppOpen(activity))
        assertFalse("canShowInterstitialNow should be false when no policy", AdController.canShowInterstitialNow())
        assertFalse("banner enabled should be false when no policy", AdController.isBannerEnabled())
    }

    @Test
    fun interstitial_counters_persist_to_sharedprefs() {
        val activity = ApplicationProvider.getApplicationContext<android.app.Application>()
        // initialize to set appContext used by persistence
        AdController.initialize(activity.applicationContext)
        // set permissive policy
        val policy = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy(
            id = 2L,
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

        // perform reserve which should persist counts
        val reserved = AdController.reserveInterstitialSlot()
        assertTrue(reserved)

        // read sharedprefs to validate stored counts
        val sp = activity.getSharedPreferences("ad_policy_counters", Context.MODE_PRIVATE)
        val interHour = sp.getInt("inter_hour_count", -999)
        val interDay = sp.getInt("inter_day_count", -999)
        assertTrue("sharedprefs should store inter_hour_count >= 0", interHour >= 0)
        assertTrue("sharedprefs should store inter_day_count >= 0", interDay >= 0)

        // cleanup: unreserve and clear prefs
        AdController.unreserveInterstitialSlot()
        sp.edit().clear().apply()
    }

    @Test
    fun fullscreen_and_splash_listeners_called() {
        var fsStateSeen: Boolean? = null
        var splashCalled = false

        val fsListener: (Boolean) -> Unit = { b -> fsStateSeen = b }
        val splashListener: () -> Unit = { splashCalled = true }

        AdController.addFullScreenShowListener(fsListener)
        AdController.addSplashReleaseListener(splashListener)

        // toggle fullscreen
        AdController.setFullScreenAdShowing(true)
        assertTrue("full-screen listener should observe true", fsStateSeen == true)

        AdController.setFullScreenAdShowing(false)
        assertTrue("full-screen listener should observe false", fsStateSeen == false)

        // trigger splash release
        AdController.triggerSplashRelease()
        assertTrue("splash release listener should be invoked", splashCalled)

        // cleanup
        AdController.removeFullScreenShowListener(fsListener)
        AdController.removeSplashReleaseListener(splashListener)
    }

    @Test
    fun counters_reset_when_policy_changed() {
        // increment app-open counters via record
        val activity = ApplicationProvider.getApplicationContext<android.app.Application>()
        AdController.recordAppOpenShown(activity)
        val snap1 = AdController.debugSnapshot()
        val appHour1 = extractInt(snap1, "appCountsHour")
        assertTrue(appHour1 >= 1)

        // change policy -> setPolicyForTest resets counters
        val policy2 = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy(
            id = 3L,
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
        AdController.setPolicyForTest(policy2)
        val snap2 = AdController.debugSnapshot()
        val appHour2 = extractInt(snap2, "appCountsHour")
        assertEquals("appCountsHour should be reset when policy is changed via setPolicyForTest", 0, appHour2)
    }
}
