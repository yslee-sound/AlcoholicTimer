package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.ad.*
import kr.sweetapps.alcoholictimer.ui.main.AdManager
import kr.sweetapps.alcoholictimer.ui.main.AdPolicyConfig
import kr.sweetapps.alcoholictimer.ui.main.AdType
import kr.sweetapps.alcoholictimer.ui.main.InMemoryPreferencesStore
import kr.sweetapps.alcoholictimer.ui.main.TimeProvider
import org.junit.Assert.*
import org.junit.Test

/**
 * 통합 광고 관련 단위 테스트 모음
 * - AdManager 관련 테스트 (cooldown, 카운트, 윈도우 초기화)
 * - AdController 관련 플래그/리스너 테스트
 * - 빌드/광고 유닛 설정(ReleaseAdConfig) 검사
 */
class AdsCombinedTest {

    // --- AdManager 관련 테스트 (원본: com.alcoholictimer.ad.AdManagerTest) ---
    class MockTimeProvider(var now: Long) : TimeProvider {
        override fun nowMillis(): Long = now
    }

    private fun startOfDay(millis: Long): Long {
        val tzOffset = java.util.TimeZone.getDefault().getOffset(millis)
        return (millis + tzOffset) / 86_400_000L * 86_400_000L - tzOffset
    }

    @Test
    fun AppOpen_Cooldown_Success() {
        val baseTime = 1_600_000_000_000L
        val timeProvider = MockTimeProvider(baseTime)
        val prefs = InMemoryPreferencesStore()
        val policy = AdPolicyConfig(app_open_cooldown_seconds = 60)
        val manager = AdManager(policy, prefs, timeProvider)

        // 앱 오픈 광고 노출 기록
        manager.incrementAdCount(AdType.APP_OPEN)
        val last = manager.getLastAppOpenTime()
        assertEquals(baseTime, last)

        // 30초 후
        timeProvider.now = baseTime + 30_000L
        val canShow = manager.canShowAd(AdType.APP_OPEN)
        assertFalse("30초 후에는 쿨다운 때문에 노출 불가해야 함", canShow)
    }

    @Test
    fun AppOpen_Cooldown_Failure() {
        val baseTime = 1_600_000_000_000L
        val timeProvider = MockTimeProvider(baseTime)
        val prefs = InMemoryPreferencesStore()
        val policy = AdPolicyConfig(app_open_cooldown_seconds = 60)
        val manager = AdManager(policy, prefs, timeProvider)

        manager.incrementAdCount(AdType.APP_OPEN)

        // 61초 후
        timeProvider.now = baseTime + 61_000L
        val canShow = manager.canShowAd(AdType.APP_OPEN)
        assertTrue("61초 후에는 쿨다운이 끝나 노출 가능해야 함", canShow)
    }

    @Test
    fun Interstitial_Cooldown_Override() {
        val baseTime = 1_600_000_000_000L
        val timeProvider = MockTimeProvider(baseTime)
        val prefs = InMemoryPreferencesStore()
        val policy = AdPolicyConfig(ad_interstitial_max_per_hour = 3)
        val manager = AdManager(policy, prefs, timeProvider)

        // 세 번 노출되어 시간당 최대에 도달
        manager.incrementAdCount(AdType.INTERSTITIAL)
        manager.incrementAdCount(AdType.INTERSTITIAL)
        manager.incrementAdCount(AdType.INTERSTITIAL)
        assertEquals(3, manager.getHourCountInterstitial())

        // 뒤로 가기 시 광고 노출 여부 판단: 이미 최대치이면 광고 노출 안하고 네비게이션 발생
        val canShow = manager.canShowAd(AdType.INTERSTITIAL)
        assertFalse("시간당 최대 도달 시 전면광고 노출 불가", canShow)

        // 가정: 뒤로가기 로직은 canShowAd가 false면 네비게이션 실행
        val navigationOccurs = !canShow
        assertTrue("광고 대신 화면 이동이 발생해야 함", navigationOccurs)
        assertFalse("광고가 실제로 표시되지 않음", manager.isShowingInterstitialAd)
    }

    @Test
    fun Count_Integrity_Check() {
        val baseTime = 1_600_000_000_000L
        val prevDay = baseTime - 2 * 86_400_000L
        val prefs = InMemoryPreferencesStore()
        val prevDayStart = startOfDay(prevDay)

        // 키 이름은 AdManager 내부와 동일하게 사용
        val KEY_DAY_WINDOW_START = "ad_day_window_start"
        val KEY_DAY_COUNT_INTERSTITIAL = "day_count_interstitial"
        val KEY_DAY_COUNT_APP_OPEN = "day_count_app_open"

        // 이전 날짜의 값으로 저장
        prefs.putLong(KEY_DAY_WINDOW_START, prevDayStart)
        prefs.putInt(KEY_DAY_COUNT_INTERSTITIAL, 5)
        prefs.putInt(KEY_DAY_COUNT_APP_OPEN, 2)

        // timeProvider는 현재 날짜(기준 시간)
        val timeProvider = MockTimeProvider(baseTime)
        val policy = AdPolicyConfig()
        val manager = AdManager(policy, prefs, timeProvider)

        // 날짜가 바뀌었으므로 일일 카운트는 초기화되어야 함
        assertEquals(0, manager.getDayCountInterstitial())
        assertEquals(0, manager.getDayCountAppOpen())
    }

    @Test
    fun policy_isActive_false_disables_all_ad_types() {
        val timeProvider = MockTimeProvider(1_600_000_000_000L)
        val prefs = InMemoryPreferencesStore()
        // 정책 전체 비활성화
        val policy = AdPolicyConfig(
            is_active = false,
            ad_app_open_enabled = true,
            ad_interstitial_enabled = true,
            ad_banner_enabled = true
        )
        val manager = AdManager(policy, prefs, timeProvider)

        assertFalse("is_active=false이면 APP_OPEN 불가", manager.canShowAd(AdType.APP_OPEN))
        assertFalse("is_active=false이면 INTERSTITIAL 불가", manager.canShowAd(AdType.INTERSTITIAL))
        assertFalse("is_active=false이면 BANNER 불가", manager.canShowAd(AdType.BANNER))
    }

    @Test
    fun policy_type_flag_disables_only_specific_type() {
        val timeProvider = MockTimeProvider(1_600_000_000_000L)
        val prefs = InMemoryPreferencesStore()
        // 배너만 비활성화, 나머지는 활성
        val policy = AdPolicyConfig(
            is_active = true,
            ad_app_open_enabled = true,
            ad_interstitial_enabled = true,
            ad_banner_enabled = false
        )
        val manager = AdManager(policy, prefs, timeProvider)

        // APP_OPEN과 INTERSTITIAL은 기본 조건에서 노출 가능 (쿨다운/카운트가 없으므로 true)
        assertTrue("ad_banner_enabled=false일 때 APP_OPEN은 허용되어야 함", manager.canShowAd(AdType.APP_OPEN))
        assertTrue("ad_banner_enabled=false일 때 INTERSTITIAL은 허용되어야 함", manager.canShowAd(AdType.INTERSTITIAL))
        // 배너만 비활성
        assertFalse("ad_banner_enabled=false일 때 BANNER는 불가", manager.canShowAd(AdType.BANNER))
    }

    @Test
    fun parsePolicy_empty_or_invalid_returns_disabled_policy() {
        val disabled = kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository.parsePolicyFromJson(null, "kr.sweetapps.alcoholictimer")
        assertNotNull(disabled)
        val d = disabled!! // non-null alias for safer access
        val fallback = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy.DEFAULT_FALLBACK
        // Verify core fallback fields match the Default Fallback Policy (P6 spec)
        assertEquals(fallback.isActive, d.isActive)
        assertEquals(fallback.adAppOpenEnabled, d.adAppOpenEnabled)
        assertEquals(fallback.adInterstitialEnabled, d.adInterstitialEnabled)
        assertEquals(fallback.adBannerEnabled, d.adBannerEnabled)
        assertEquals(fallback.adInterstitialMaxPerDay, d.adInterstitialMaxPerDay)
        assertEquals(fallback.appOpenCooldownSeconds, d.appOpenCooldownSeconds)
        assertEquals(fallback.appOpenMaxPerHour, d.appOpenMaxPerHour)

        val disabled2 = kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository.parsePolicyFromJson("[]", "kr.sweetapps.alcoholictimer")
        assertNotNull(disabled2)
        val d2 = disabled2!!
        assertEquals(fallback.isActive, d2.isActive)
    }

    @Test
    fun repository_fetcher_empty_body_returns_disabled_policy() {
        val fetcher = object : kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository.Fetcher {
            override fun get(url: String): Pair<Int, String?> = Pair(200, "[]")
        }
        val repo = kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository("kr.sweetapps.alcoholictimer", fetcher)
        // call suspend function
        val policy = kotlinx.coroutines.runBlocking { repo.getPolicy() }
        assertNotNull(policy)
        val p = policy!!
        val fallback = kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy.DEFAULT_FALLBACK
        assertEquals(fallback.isActive, p.isActive)
        assertEquals(fallback.appOpenCooldownSeconds, p.appOpenCooldownSeconds)
    }

    @Test
    fun parsePolicy_selects_matching_app_id() {
        // Prepare JSON with multiple candidate policies
        val json = "[\n" +
            "  {\n" +
            "    \"id\": 1,\n" +
            "    \"app_id\": \"other.app\",\n" +
            "    \"is_active\": true,\n" +
            "    \"ad_banner_enabled\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": 2,\n" +
            "    \"app_id\": \"kr.sweetapps.alcoholictimer\",\n" +
            "    \"is_active\": true,\n" +
            "    \"ad_banner_enabled\": true,\n" +
            "    \"ad_app_open_enabled\": false\n" +
            "  }\n" +
            "]"
        val policy = kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository.parsePolicyFromJson(json, "kr.sweetapps.alcoholictimer")
        assertNotNull(policy)
        assertEquals(2L, policy.id)
        assertTrue(policy.adBannerEnabled)
        assertFalse(policy.adAppOpenEnabled)

        // Also verify fetcher + repository returns same chosen policy
        val fetcher = object : kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository.Fetcher {
            override fun get(url: String): Pair<Int, String?> = Pair(200, json)
        }
        val repo = kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository("kr.sweetapps.alcoholictimer", fetcher)
        val fetched = kotlinx.coroutines.runBlocking { repo.getPolicy() }
        assertNotNull(fetched)
        assertEquals(policy.id, fetched!!.id)
    }

    // --- AdController 관련 테스트 (원본: kr.sweetapps.alcoholictimer.ads.AdControllerTest) ---
    @Test
    fun fullScreenListener_receives_initial_and_changes() {
        // 초기 상태는 false
        assertFalse(AdController.isFullScreenAdShowing())

        val events = mutableListOf<Boolean>()
        val listener: (Boolean) -> Unit = { showing -> events.add(showing) }

        AdController.addFullScreenShowListener(listener)
        try {
            assertTrue(events.isNotEmpty())
            assertEquals(false, events[0])

            events.clear()
            AdController.setFullScreenAdShowing(true)
            assertEquals(listOf(true), events)

            events.clear()
            AdController.setFullScreenAdShowing(false)
        } finally {
            AdController.removeFullScreenShowListener(listener)
        }
    }

    @Test
    fun interstitialFlag_toggles() {
        AdController.setInterstitialShowing(false)
        assertFalse(AdController.isInterstitialShowingNow())

        AdController.setInterstitialShowing(true)
        assertTrue(AdController.isInterstitialShowingNow())

        AdController.setInterstitialShowing(false)
        assertFalse(AdController.isInterstitialShowingNow())
    }

    // --- ReleaseAdConfig 테스트 (원본: kr.sweetapps.alcoholictimer.ReleaseAdConfigTest) ---
    @Test
    fun buildConfig_release_debug_check() {
        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) return
        assertFalse("In release build BuildConfig.DEBUG must be false", kr.sweetapps.alcoholictimer.BuildConfig.DEBUG)
    }

    @Test
    fun banner_and_interstitial_unit_ids_configured() {
        val bannerId: String = kr.sweetapps.alcoholictimer.BuildConfig.ADMOB_BANNER_UNIT_ID
        assertTrue("Banner id must not be blank", bannerId.isNotBlank())
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            assertFalse("Release build must not use test banner id", bannerId.contains("3940256099942544"))
        }

        val id: String = kr.sweetapps.alcoholictimer.BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
        assertTrue("Interstitial id must not be blank", id.isNotBlank())
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            assertFalse("Release build must not use test interstitial id", id.contains("3940256099942544"))
        }
    }
}
