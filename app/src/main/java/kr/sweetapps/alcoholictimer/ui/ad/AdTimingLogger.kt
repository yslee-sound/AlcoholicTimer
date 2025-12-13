package kr.sweetapps.alcoholictimer.ui.ad

import android.util.Log

object AdTimingLogger {
    private const val TAG = "AdTimingDiagnosis"

    // ???�작 ?�각 (Application.onCreate)
    @Volatile
    private var appStartTimeMs: Long = 0L

    // 배너 광고 로드 ?�청 ?�각
    @Volatile
    private var bannerLoadRequestTimeMs: Long = 0L

    // 배너 광고 로드 ?�료 ?�각
    @Volatile
    private var bannerLoadCompleteTimeMs: Long = 0L

    // AppOpen 광고 로드 ?�청 ?�각
    @Volatile
    private var appOpenLoadRequestTimeMs: Long = 0L

    // AppOpen 광고 로드 ?�료 ?�각
    @Volatile
    private var appOpenLoadCompleteTimeMs: Long = 0L

    // MainActivity 진입 ?�각
    @Volatile
    private var mainActivityCreateTimeMs: Long = 0L

    // SplashScreen ?�성 ?�각
    @Volatile
    private var splashScreenCreateTimeMs: Long = 0L

    // SplashScreen 종료 ?�각
    @Volatile
    private var splashScreenFinishTimeMs: Long = 0L

    /**
     * ???�작 ?�각 기록 (Application.onCreate)
     */
    fun logAppStart() {
        appStartTimeMs = System.currentTimeMillis()
        Log.d(TAG, "?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═")
        Log.d(TAG, "?�� APP START: t=0ms (${appStartTimeMs})")
        Log.d(TAG, "?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═")
    }

    /**
     * SplashScreen ?�성 ?�각
     */
    fun logSplashScreenCreate() {
        splashScreenCreateTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenCreateTimeMs - appStartTimeMs
        Log.d(TAG, "?�� SPLASH SCREEN CREATED: t+${elapsed}ms")
    }

    /**
     * SplashScreen 종료 ?�각
     */
    fun logSplashScreenFinish() {
        splashScreenFinishTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenFinishTimeMs - appStartTimeMs
        Log.d(TAG, "?�� SPLASH SCREEN FINISHED: t+${elapsed}ms")
    }

    /**
     * MainActivity 진입 ?�각 기록
     */
    fun logMainActivityCreate() {
        mainActivityCreateTimeMs = System.currentTimeMillis()
        val elapsed = mainActivityCreateTimeMs - appStartTimeMs
        Log.d(TAG, "?�� MAIN ACTIVITY CREATED: t+${elapsed}ms")

        if (bannerLoadCompleteTimeMs > 0 && mainActivityCreateTimeMs < bannerLoadCompleteTimeMs) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            Log.w(TAG, "?�️ TIMING ISSUE: MainActivity created ${gap}ms BEFORE banner loaded!")
            Log.w(TAG, "?�️ This means banner had NO CHANCE to be shown!")
        }
    }


    fun logBannerLoadRequest() {
        bannerLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "?�� BANNER LOAD REQUESTED: t+${elapsed}ms")
    }


    fun logBannerLoadComplete(isActivityFinishing: Boolean = false) {
        bannerLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (bannerLoadRequestTimeMs > 0) {
            bannerLoadCompleteTimeMs - bannerLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "??BANNER LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // ?�재 Activity ?�태 ?�인
        if (isActivityFinishing) {
            Log.w(TAG, "?�️ Activity is FINISHING - banner loaded too late!")
        }

        // MainActivity?� 비교
        if (mainActivityCreateTimeMs > 0) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            if (gap > 0) {
                Log.w(TAG, "?�️ Banner loaded ${gap}ms AFTER MainActivity created")
                Log.w(TAG, "?�️ DIAGNOSIS: Banner missed display opportunity due to late loading")
            } else {
                Log.d(TAG, "??Banner loaded ${-gap}ms BEFORE MainActivity created (good timing)")
            }
        }
    }

    /**
     * AppOpen 광고 로드 ?�청 ?�각 기록
     */
    fun logAppOpenLoadRequest() {
        appOpenLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "?? APP OPEN AD LOAD REQUESTED: t+${elapsed}ms")
    }


    fun logAppOpenLoadComplete() {
        appOpenLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (appOpenLoadRequestTimeMs > 0) {
            appOpenLoadCompleteTimeMs - appOpenLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "??APP OPEN AD LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // SplashScreen???��? 종료?�었?��? ?�인
        if (splashScreenFinishTimeMs > 0 && appOpenLoadCompleteTimeMs > splashScreenFinishTimeMs) {
            val gap = appOpenLoadCompleteTimeMs - splashScreenFinishTimeMs
            Log.w(TAG, "?�️ AppOpen loaded ${gap}ms AFTER SplashScreen finished")
            Log.w(TAG, "?�️ DIAGNOSIS: AppOpen ad missed display opportunity")
        }
    }


    fun printTimingReport() {
        Log.d(TAG, "?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═")
        Log.d(TAG, "?�� AD TIMING DIAGNOSIS REPORT")
        Log.d(TAG, "?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═")

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

        // Banner ?�?�밍 분석
        if (bannerLoadCompleteTimeMs > 0 && mainActivityCreateTimeMs > 0) {
            val gap = mainActivityCreateTimeMs - bannerLoadCompleteTimeMs
            if (gap < 0) {
                Log.w(TAG, "  ??PROBLEM: Banner loaded ${-gap}ms AFTER MainActivity")
                Log.w(TAG, "  ??Banner had no chance to be displayed")
                Log.w(TAG, "  ??This explains why AdMob shows requests but 0 impressions")
            } else {
                Log.d(TAG, "  ??OK: Banner loaded ${gap}ms BEFORE MainActivity")
            }
        }

        // AppOpen ?�?�밍 분석
        if (appOpenLoadCompleteTimeMs > 0 && splashScreenFinishTimeMs > 0) {
            val gap = splashScreenFinishTimeMs - appOpenLoadCompleteTimeMs
            if (gap < 0) {
                Log.w(TAG, "  ??PROBLEM: AppOpen loaded ${-gap}ms AFTER SplashScreen finished")
                Log.w(TAG, "  ??AppOpen ad missed display opportunity")
            } else {
                Log.d(TAG, "  ??OK: AppOpen loaded ${gap}ms BEFORE SplashScreen finished")
            }
        }

        Log.d(TAG, "?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═?�═")
    }

    /**
     * ?�계 리셋 (?�스?�용)
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

