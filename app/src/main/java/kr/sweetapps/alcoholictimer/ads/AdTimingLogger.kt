package kr.sweetapps.alcoholictimer.ads

import android.util.Log

/**
 * 광고 타이밍 진단을 위한 로거
 *
 * 목적: AdMob 대시보드에서 '요청 수'는 잡히지만 '노출 수'가 0인 문제 진단
 * 가설: 광고 로드가 완료되기 전에 이미 화면 전환이 완료되어 노출 기회를 놓친다
 */
object AdTimingLogger {
    private const val TAG = "AdTimingDiagnosis"

    // 앱 시작 시각 (Application.onCreate)
    @Volatile
    private var appStartTimeMs: Long = 0L

    // 배너 광고 로드 요청 시각
    @Volatile
    private var bannerLoadRequestTimeMs: Long = 0L

    // 배너 광고 로드 완료 시각
    @Volatile
    private var bannerLoadCompleteTimeMs: Long = 0L

    // AppOpen 광고 로드 요청 시각
    @Volatile
    private var appOpenLoadRequestTimeMs: Long = 0L

    // AppOpen 광고 로드 완료 시각
    @Volatile
    private var appOpenLoadCompleteTimeMs: Long = 0L

    // MainActivity 진입 시각
    @Volatile
    private var mainActivityCreateTimeMs: Long = 0L

    // SplashScreen 생성 시각
    @Volatile
    private var splashScreenCreateTimeMs: Long = 0L

    // SplashScreen 종료 시각
    @Volatile
    private var splashScreenFinishTimeMs: Long = 0L

    /**
     * 앱 시작 시각 기록 (Application.onCreate)
     */
    fun logAppStart() {
        appStartTimeMs = System.currentTimeMillis()
        Log.d(TAG, "════════════════════════════════════════════════════════")
        Log.d(TAG, "📱 APP START: t=0ms (${appStartTimeMs})")
        Log.d(TAG, "════════════════════════════════════════════════════════")
    }

    /**
     * SplashScreen 생성 시각
     */
    fun logSplashScreenCreate() {
        splashScreenCreateTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenCreateTimeMs - appStartTimeMs
        Log.d(TAG, "🎬 SPLASH SCREEN CREATED: t+${elapsed}ms")
    }

    /**
     * SplashScreen 종료 시각
     */
    fun logSplashScreenFinish() {
        splashScreenFinishTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenFinishTimeMs - appStartTimeMs
        Log.d(TAG, "👋 SPLASH SCREEN FINISHED: t+${elapsed}ms")
    }

    /**
     * MainActivity 진입 시각 기록
     */
    fun logMainActivityCreate() {
        mainActivityCreateTimeMs = System.currentTimeMillis()
        val elapsed = mainActivityCreateTimeMs - appStartTimeMs
        Log.d(TAG, "🏠 MAIN ACTIVITY CREATED: t+${elapsed}ms")

        // 가설 검증: MainActivity가 배너 로드보다 먼저 생성되었는지 확인
        if (bannerLoadCompleteTimeMs > 0 && mainActivityCreateTimeMs < bannerLoadCompleteTimeMs) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            Log.w(TAG, "⚠️ TIMING ISSUE: MainActivity created ${gap}ms BEFORE banner loaded!")
            Log.w(TAG, "⚠️ This means banner had NO CHANCE to be shown!")
        }
    }

    /**
     * 배너 광고 로드 요청 시각 기록
     */
    fun logBannerLoadRequest() {
        bannerLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "🎯 BANNER LOAD REQUESTED: t+${elapsed}ms")
    }

    /**
     * 배너 광고 로드 완료 시각 기록
     */
    fun logBannerLoadComplete(isActivityFinishing: Boolean = false) {
        bannerLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (bannerLoadRequestTimeMs > 0) {
            bannerLoadCompleteTimeMs - bannerLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "✅ BANNER LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // 현재 Activity 상태 확인
        if (isActivityFinishing) {
            Log.w(TAG, "⚠️ Activity is FINISHING - banner loaded too late!")
        }

        // MainActivity와 비교
        if (mainActivityCreateTimeMs > 0) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            if (gap > 0) {
                Log.w(TAG, "⚠️ Banner loaded ${gap}ms AFTER MainActivity created")
                Log.w(TAG, "⚠️ DIAGNOSIS: Banner missed display opportunity due to late loading")
            } else {
                Log.d(TAG, "✓ Banner loaded ${-gap}ms BEFORE MainActivity created (good timing)")
            }
        }
    }

    /**
     * AppOpen 광고 로드 요청 시각 기록
     */
    fun logAppOpenLoadRequest() {
        appOpenLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "🚀 APP OPEN AD LOAD REQUESTED: t+${elapsed}ms")
    }

    /**
     * AppOpen 광고 로드 완료 시각 기록
     */
    fun logAppOpenLoadComplete() {
        appOpenLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (appOpenLoadRequestTimeMs > 0) {
            appOpenLoadCompleteTimeMs - appOpenLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "✅ APP OPEN AD LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // SplashScreen이 이미 종료되었는지 확인
        if (splashScreenFinishTimeMs > 0 && appOpenLoadCompleteTimeMs > splashScreenFinishTimeMs) {
            val gap = appOpenLoadCompleteTimeMs - splashScreenFinishTimeMs
            Log.w(TAG, "⚠️ AppOpen loaded ${gap}ms AFTER SplashScreen finished")
            Log.w(TAG, "⚠️ DIAGNOSIS: AppOpen ad missed display opportunity")
        }
    }

    /**
     * 최종 타이밍 리포트 출력
     */
    fun printTimingReport() {
        Log.d(TAG, "════════════════════════════════════════════════════════")
        Log.d(TAG, "📊 AD TIMING DIAGNOSIS REPORT")
        Log.d(TAG, "════════════════════════════════════════════════════════")

        if (appStartTimeMs == 0L) {
            Log.d(TAG, "No timing data recorded yet")
            return
        }

        Log.d(TAG, "Timeline (all times relative to app start):")
        Log.d(TAG, "  0ms: App started")

        if (splashScreenCreateTimeMs > 0) {
            Log.d(TAG, "  ${splashScreenCreateTimeMs - appStartTimeMs}ms: SplashScreen created")
        }

        if (appOpenLoadRequestTimeMs > 0) {
            Log.d(TAG, "  ${appOpenLoadRequestTimeMs - appStartTimeMs}ms: AppOpen load requested")
        }

        if (bannerLoadRequestTimeMs > 0) {
            Log.d(TAG, "  ${bannerLoadRequestTimeMs - appStartTimeMs}ms: Banner load requested")
        }

        if (appOpenLoadCompleteTimeMs > 0) {
            Log.d(TAG, "  ${appOpenLoadCompleteTimeMs - appStartTimeMs}ms: AppOpen loaded")
        }

        if (bannerLoadCompleteTimeMs > 0) {
            Log.d(TAG, "  ${bannerLoadCompleteTimeMs - appStartTimeMs}ms: Banner loaded")
        }

        if (mainActivityCreateTimeMs > 0) {
            Log.d(TAG, "  ${mainActivityCreateTimeMs - appStartTimeMs}ms: MainActivity created")
        }

        if (splashScreenFinishTimeMs > 0) {
            Log.d(TAG, "  ${splashScreenFinishTimeMs - appStartTimeMs}ms: SplashScreen finished")
        }

        Log.d(TAG, "")
        Log.d(TAG, "Analysis:")

        // Banner 타이밍 분석
        if (bannerLoadCompleteTimeMs > 0 && mainActivityCreateTimeMs > 0) {
            val gap = mainActivityCreateTimeMs - bannerLoadCompleteTimeMs
            if (gap < 0) {
                Log.w(TAG, "  ❌ PROBLEM: Banner loaded ${-gap}ms AFTER MainActivity")
                Log.w(TAG, "  → Banner had no chance to be displayed")
                Log.w(TAG, "  → This explains why AdMob shows requests but 0 impressions")
            } else {
                Log.d(TAG, "  ✓ OK: Banner loaded ${gap}ms BEFORE MainActivity")
            }
        }

        // AppOpen 타이밍 분석
        if (appOpenLoadCompleteTimeMs > 0 && splashScreenFinishTimeMs > 0) {
            val gap = splashScreenFinishTimeMs - appOpenLoadCompleteTimeMs
            if (gap < 0) {
                Log.w(TAG, "  ❌ PROBLEM: AppOpen loaded ${-gap}ms AFTER SplashScreen finished")
                Log.w(TAG, "  → AppOpen ad missed display opportunity")
            } else {
                Log.d(TAG, "  ✓ OK: AppOpen loaded ${gap}ms BEFORE SplashScreen finished")
            }
        }

        Log.d(TAG, "════════════════════════════════════════════════════════")
    }

    /**
     * 통계 리셋 (테스트용)
     */
    fun reset() {
        appStartTimeMs = 0L
        bannerLoadRequestTimeMs = 0L
        bannerLoadCompleteTimeMs = 0L
        appOpenLoadRequestTimeMs = 0L
        appOpenLoadCompleteTimeMs = 0L
        mainActivityCreateTimeMs = 0L
        splashScreenCreateTimeMs = 0L
        splashScreenFinishTimeMs = 0L
    }
}

