package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.ad.AdManager
import kr.sweetapps.alcoholictimer.ad.AdPolicyConfig
import kr.sweetapps.alcoholictimer.ad.AdType
import kr.sweetapps.alcoholictimer.ad.InMemoryPreferencesStore
import kr.sweetapps.alcoholictimer.ad.TimeProvider
import kr.sweetapps.alcoholictimer.popup.MockPolicyRepository
import kr.sweetapps.alcoholictimer.popup.MockSharedPreferences
import kr.sweetapps.alcoholictimer.popup.MockSystemInfo
import kr.sweetapps.alcoholictimer.popup.MockTimeProvider
import kr.sweetapps.alcoholictimer.popup.PopupManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntegrationAdPopupTest {
    private lateinit var repo: MockPolicyRepository
    private lateinit var prefs: MockSharedPreferences
    private lateinit var sys: MockSystemInfo
    private lateinit var time: MockTimeProvider
    private lateinit var manager: PopupManager

    class TP(var now: Long) : TimeProvider { override fun nowMillis(): Long = now }

    @Before
    fun setup() {
        repo = MockPolicyRepository()
        prefs = MockSharedPreferences()
        sys = MockSystemInfo(currentVersionCode = 100)
        time = MockTimeProvider(nowMillis = 1_600_000_000_000L)
        manager = PopupManager(repo, prefs, sys, time)
        // ensure ad policy permissive for tests
        val policy = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy(
            id = 999L,
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
    }

    @Test
    fun popupFlag_blocks_appOpen_and_interstitial() {
        // 초기 상태: 플래그 해제
        AdController.setFullScreenAdShowing(false)

        val base = 1_600_000_000_000L
        val tp = TP(base)
        val prefs = InMemoryPreferencesStore()
        val policy = AdPolicyConfig() // 기본값은 활성
        val manager = AdManager(policy, prefs, tp)

        // 기본적으로 노출 가능
        assertTrue(manager.canShowAd(AdType.APP_OPEN))
        assertTrue(manager.canShowAd(AdType.INTERSTITIAL))

        // 팝업 표시 중으로 설정 -> 광고는 불가
        AdController.setFullScreenAdShowing(true)
        assertFalse("팝업 활성 중에는 APP_OPEN 노출 불가", manager.canShowAd(AdType.APP_OPEN))
        assertFalse("팝업 활성 중에는 INTERSTITIAL 노출 불가", manager.canShowAd(AdType.INTERSTITIAL))

        // 팝업 해제 -> 다시 노출 가능
        AdController.setFullScreenAdShowing(false)
        assertTrue(manager.canShowAd(AdType.APP_OPEN))
        assertTrue(manager.canShowAd(AdType.INTERSTITIAL))
    }

    @Test
    fun popup_show_sets_fullscreen_flag_and_blocks_interstitial() {
        // arrange: emergency popup active
        repo.setMockEmergency(kr.sweetapps.alcoholictimer.popup.EmergencyPolicyConfig(app_id = "kr.sweetapps.alcoholictimer", is_active = true, content = "E", redirect_url = null, button_text = null, is_dismissible = true))

        // act: determine popup to show (this should set full-screen flag)
        val res = manager.getPopupToShow()
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.Emergency)

        // confirm AdController reports full-screen showing
        assertTrue("AdController should have full-screen flag set after popup shown", AdController.isFullScreenAdShowing())

        // attempt to show interstitial -> should be blocked
        val willShow = AdController.canShowInterstitialNow() && !AdController.isFullScreenAdShowing()
        assertFalse("Interstitial should be blocked while full-screen popup is active", willShow)
    }

    @Test
    fun popup_dismiss_clears_fullscreen_flag_and_allows_interstitial() {
        // show emergency first
        repo.setMockEmergency(kr.sweetapps.alcoholictimer.popup.EmergencyPolicyConfig(app_id = "kr.sweetapps.alcoholictimer", is_active = true, content = "E", redirect_url = null, button_text = null, is_dismissible = true))
        manager.getPopupToShow()
        assertTrue(AdController.isFullScreenAdShowing())

        // simulate popup dismissed: repo returns no emergency and manager recomputes
        repo.setMockEmergency(null)
        val res2 = manager.getPopupToShow()
        assertTrue(res2 is kr.sweetapps.alcoholictimer.popup.PopupResult.None)
        // manager should have cleared flag
        assertFalse("AdController full-screen flag should be cleared after popup no longer present", AdController.isFullScreenAdShowing())

        // now interstitial should be allowed (policy is permissive)
        assertTrue("Interstitial should be allowed after popup dismissed", AdController.canShowInterstitialNow() && !AdController.isFullScreenAdShowing())
    }

    @Test
    fun popup_update_force_sets_fullscreen_and_blocks_interstitial() {
        // arrange: force-update active (target_version higher than current)
        repo.setMockUpdate(kr.sweetapps.alcoholictimer.popup.UpdatePolicyConfig(
            app_id = "kr.sweetapps.alcoholictimer",
            is_active = true,
            target_version_code = 200,
            is_force_update = true,
            release_notes = null,
            download_url = null,
            reshow_interval_hours = 24,
            max_later_count = 2
        ))

        // act
        val res = manager.getPopupToShow()
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.Update)

        // AdController should reflect full-screen popup
        assertTrue("AdController should set full-screen flag for force update", AdController.isFullScreenAdShowing())

        // interstitial must be blocked while popup active
        val willShow = AdController.canShowInterstitialNow() && !AdController.isFullScreenAdShowing()
        assertFalse("Interstitial should be blocked while force-update popup is active", willShow)
    }

    @Test
    fun popup_update_nonforce_respects_later_count_and_sets_fullscreen_when_due() {
        // arrange: non-force update but reshow conditions met
        repo.setMockUpdate(kr.sweetapps.alcoholictimer.popup.UpdatePolicyConfig(
            app_id = "kr.sweetapps.alcoholictimer",
            is_active = true,
            target_version_code = 200,
            is_force_update = false,
            release_notes = null,
            download_url = null,
            reshow_interval_hours = 1,
            max_later_count = 2
        ))
        // ensure prefs indicate user hasn't postponed too many times and lastLaterTime is older than interval
        prefs.laterCount = 0
        prefs.lastLaterTimeMillis = time.nowMillis - 2 * 3600_000L // 2 hours ago

        // act
        val res = manager.getPopupToShow()
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.Update)
        // full-screen should be set for update dialog
        assertTrue("AdController should set full-screen flag for non-force update when due", AdController.isFullScreenAdShowing())

        // interstitial blocked while popup active
        val willShow = AdController.canShowInterstitialNow() && !AdController.isFullScreenAdShowing()
        assertFalse("Interstitial should be blocked while non-force update popup is active", willShow)
    }

    @Test
    fun popup_update_nonforce_not_due_does_not_set_fullscreen() {
        // arrange: non-force update but reshow interval not yet elapsed
        repo.setMockUpdate(kr.sweetapps.alcoholictimer.popup.UpdatePolicyConfig(
            app_id = "kr.sweetapps.alcoholictimer",
            is_active = true,
            target_version_code = 200,
            is_force_update = false,
            release_notes = null,
            download_url = null,
            reshow_interval_hours = 24,
            max_later_count = 2
        ))
        // user postponed recently (1 hour ago), interval is 24h -> not due
        prefs.laterCount = 0
        prefs.lastLaterTimeMillis = time.nowMillis - 1 * 3600_000L

        // act
        val res = manager.getPopupToShow()
        // should not show update yet
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.None)
        assertFalse("AdController should NOT set full-screen flag when update not due", AdController.isFullScreenAdShowing())
    }

    @Test
    fun popup_notice_does_not_set_fullscreen_and_allows_interstitial() {
        // arrange: active notice with higher version than last seen
        repo.setMockNotice(kr.sweetapps.alcoholictimer.popup.NoticePolicyConfig(
            app_id = "kr.sweetapps.alcoholictimer",
            is_active = true,
            title = "N",
            content = "Notice",
            notice_version = 5
        ))

        // act
        val res = manager.getPopupToShow()
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.Notice)

        // Notice should NOT set full-screen flag (PopupManager treats notice as non-fullscreen in tests)
        assertFalse("AdController should NOT set full-screen flag for notice by default", AdController.isFullScreenAdShowing())

        // interstitial may be allowed; ensure call does not crash and returns boolean
        assertTrue("Interstitial should be allowed for notice", AdController.canShowInterstitialNow() && !AdController.isFullScreenAdShowing())
    }

    @Test
    fun popup_notice_already_seen_skips() {
        // arrange: notice with same version as last seen
        prefs.lastSeenNoticeVersion = 5
        repo.setMockNotice(kr.sweetapps.alcoholictimer.popup.NoticePolicyConfig(
            app_id = "kr.sweetapps.alcoholictimer",
            is_active = true,
            title = "N",
            content = "Notice",
            notice_version = 5
        ))

        // act
        val res = manager.getPopupToShow()
        // should not show notice since user already saw this version
        assertTrue(res is kr.sweetapps.alcoholictimer.popup.PopupResult.None)
        assertFalse("AdController should NOT set full-screen flag for already-seen notice", AdController.isFullScreenAdShowing())
    }
}
