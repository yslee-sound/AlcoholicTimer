// Unit tests for PopupManager
package kr.sweetapps.alcoholictimer.popup

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PopupManagerTest {
    private lateinit var repo: MockPolicyRepository
    private lateinit var prefs: MockSharedPreferences
    private lateinit var sys: MockSystemInfo
    private lateinit var time: MockTimeProvider
    private lateinit var manager: PopupManager

    @Before
    fun setup() {
        repo = MockPolicyRepository()
        prefs = MockSharedPreferences()
        sys = MockSystemInfo(currentVersionCode = 100)
        time = MockTimeProvider(nowMillis = 1_600_000_000_000L) // arbitrary
        manager = PopupManager(repo, prefs, sys, time)
    }

    @Test
    fun Priority_Emergency_Override() {
        repo.setMockEmergency(
            EmergencyPolicyConfig(app_id = "com.example", is_active = true, content = "EMER", redirect_url = "https://x", button_text = "OK", is_dismissible = true)
        )
        repo.setMockNotice(
            NoticePolicyConfig(app_id = "com.example", is_active = true, title = "T", content = "N", notice_version = 1)
        )

        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Emergency)
        val em = (res as PopupResult.Emergency).policy
        assertEquals("EMER", em.content)
    }

    @Test
    fun Priority_ForceUpdate_Override() {
        repo.setMockEmergency(null)
        repo.setMockUpdate(
            UpdatePolicyConfig(
                app_id = "com.example",
                is_active = true,
                target_version_code = 200,
                is_force_update = true,
                release_notes = "",
                download_url = "https://x",
                reshow_interval_hours = 24,
                max_later_count = 2
            )
        )
        repo.setMockNotice(
            NoticePolicyConfig(app_id = "com.example", is_active = true, title = null, content = "N", notice_version = 1)
        )

        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Update)
    }

    @Test
    fun Policy_Inactive_Check() {
        repo.setMockEmergency(EmergencyPolicyConfig("com.example", false, "", null, null, true))
        repo.setMockNotice(NoticePolicyConfig("com.example", false, null, "", 1))
        repo.setMockUpdate(UpdatePolicyConfig("com.example", false, 200, false, null, null, 24, 2))

        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.None)
    }

    @Test
    fun Policy_Data_Failure() {
        repo.setFail(true)
        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.None)
    }

    @Test
    fun Update_Version_Check() {
        // currentVersionCode = 100 from setup
        repo.setMockUpdate(UpdatePolicyConfig("com.example", true, 100, false, null, null, 24, 2))
        val res = manager.getPopupToShow()
        // target == current -> should not show (manager uses > current unless force)
        assertTrue(res is PopupResult.None)
    }

    @Test
    fun Update_MaxLater_Check() {
        repo.setMockUpdate(UpdatePolicyConfig("com.example", true, 200, false, null, null, 24, 2))
        prefs.laterCount = 2 // reached max
        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Update)
    }

    @Test
    fun Update_Reshow_Interval() {
        // set update targeting newer version
        repo.setMockUpdate(UpdatePolicyConfig("com.example", true, 200, false, null, null, 24, 5))
        // user clicked later recently
        prefs.laterCount = 1
        prefs.lastLaterTimeMillis = time.nowMillis
        // now not advanced -> should NOT show
        var res = manager.getPopupToShow()
        assertTrue(res is PopupResult.None)

        // advance beyond interval
        time.advanceMillis(25 * 3600_000L)
        res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Update)
    }

    @Test
    fun Notice_Version_Check() {
        repo.setMockNotice(NoticePolicyConfig("com.example", true, "T", "C", 4))
        prefs.lastSeenNoticeVersion = 5
        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.None)
    }

    @Test
    fun Emergency_Dismissible_Checks() {
        repo.setMockEmergency(EmergencyPolicyConfig("com.example", true, "EM", null, "OK", false))
        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Emergency)
        val em = (res as PopupResult.Emergency).policy
        assertFalse(em.is_dismissible)

        // now dismissible true
        repo.setMockEmergency(EmergencyPolicyConfig("com.example", true, "EM2", null, "OK2", true))
        val res2 = manager.getPopupToShow()
        assertTrue(res2 is PopupResult.Emergency)
        val em2 = (res2 as PopupResult.Emergency).policy
        assertTrue(em2.is_dismissible)
    }

    @Test
    fun Emergency_Content_Check() {
        repo.setMockEmergency(EmergencyPolicyConfig("com.example", true, "content-x", "https://r", "btn", true))
        val res = manager.getPopupToShow()
        assertTrue(res is PopupResult.Emergency)
        val em = (res as PopupResult.Emergency).policy
        assertEquals("content-x", em.content)
        assertEquals("https://r", em.redirect_url)
        assertEquals("btn", em.button_text)
    }
}

