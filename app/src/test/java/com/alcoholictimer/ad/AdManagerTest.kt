package com.alcoholictimer.ad

import org.junit.Assert.*
import org.junit.Test

class MockTimeProvider(var now: Long) : TimeProvider {
    override fun nowMillis(): Long = now
}

class AdManagerTest {
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
}

