package kr.sweetapps.alcoholictimer.ui.ad

import android.util.Log

/**
 * ê´‘ê³  ?€?´ë° ì§„ë‹¨???„í•œ ë¡œê±°
 *
 * ëª©ì : AdMob ?€?œë³´?œì—??'?”ì²­ ?????¡ížˆì§€ë§?'?¸ì¶œ ??ê°€ 0??ë¬¸ì œ ì§„ë‹¨
 * ê°€?? ê´‘ê³  ë¡œë“œê°€ ?„ë£Œ?˜ê¸° ?„ì— ?´ë? ?”ë©´ ?„í™˜???„ë£Œ?˜ì–´ ?¸ì¶œ ê¸°íšŒë¥??“ì¹œ??
 */
object AdTimingLogger {
    private const val TAG = "AdTimingDiagnosis"

    // ???œìž‘ ?œê° (Application.onCreate)
    @Volatile
    private var appStartTimeMs: Long = 0L

    // ë°°ë„ˆ ê´‘ê³  ë¡œë“œ ?”ì²­ ?œê°
    @Volatile
    private var bannerLoadRequestTimeMs: Long = 0L

    // ë°°ë„ˆ ê´‘ê³  ë¡œë“œ ?„ë£Œ ?œê°
    @Volatile
    private var bannerLoadCompleteTimeMs: Long = 0L

    // AppOpen ê´‘ê³  ë¡œë“œ ?”ì²­ ?œê°
    @Volatile
    private var appOpenLoadRequestTimeMs: Long = 0L

    // AppOpen ê´‘ê³  ë¡œë“œ ?„ë£Œ ?œê°
    @Volatile
    private var appOpenLoadCompleteTimeMs: Long = 0L

    // MainActivity ì§„ìž… ?œê°
    @Volatile
    private var mainActivityCreateTimeMs: Long = 0L

    // SplashScreen ?ì„± ?œê°
    @Volatile
    private var splashScreenCreateTimeMs: Long = 0L

    // SplashScreen ì¢…ë£Œ ?œê°
    @Volatile
    private var splashScreenFinishTimeMs: Long = 0L

    /**
     * ???œìž‘ ?œê° ê¸°ë¡ (Application.onCreate)
     */
    fun logAppStart() {
        appStartTimeMs = System.currentTimeMillis()
        Log.d(TAG, "?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•")
        Log.d(TAG, "?“± APP START: t=0ms (${appStartTimeMs})")
        Log.d(TAG, "?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•")
    }

    /**
     * SplashScreen ?ì„± ?œê°
     */
    fun logSplashScreenCreate() {
        splashScreenCreateTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenCreateTimeMs - appStartTimeMs
        Log.d(TAG, "?Ž¬ SPLASH SCREEN CREATED: t+${elapsed}ms")
    }

    /**
     * SplashScreen ì¢…ë£Œ ?œê°
     */
    fun logSplashScreenFinish() {
        splashScreenFinishTimeMs = System.currentTimeMillis()
        val elapsed = splashScreenFinishTimeMs - appStartTimeMs
        Log.d(TAG, "?‘‹ SPLASH SCREEN FINISHED: t+${elapsed}ms")
    }

    /**
     * MainActivity ì§„ìž… ?œê° ê¸°ë¡
     */
    fun logMainActivityCreate() {
        mainActivityCreateTimeMs = System.currentTimeMillis()
        val elapsed = mainActivityCreateTimeMs - appStartTimeMs
        Log.d(TAG, "?  MAIN ACTIVITY CREATED: t+${elapsed}ms")

        // ê°€??ê²€ì¦? MainActivityê°€ ë°°ë„ˆ ë¡œë“œë³´ë‹¤ ë¨¼ì? ?ì„±?˜ì—ˆ?”ì? ?•ì¸
        if (bannerLoadCompleteTimeMs > 0 && mainActivityCreateTimeMs < bannerLoadCompleteTimeMs) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            Log.w(TAG, "? ï¸ TIMING ISSUE: MainActivity created ${gap}ms BEFORE banner loaded!")
            Log.w(TAG, "? ï¸ This means banner had NO CHANCE to be shown!")
        }
    }

    /**
     * ë°°ë„ˆ ê´‘ê³  ë¡œë“œ ?”ì²­ ?œê° ê¸°ë¡
     */
    fun logBannerLoadRequest() {
        bannerLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "?Ž¯ BANNER LOAD REQUESTED: t+${elapsed}ms")
    }

    /**
     * ë°°ë„ˆ ê´‘ê³  ë¡œë“œ ?„ë£Œ ?œê° ê¸°ë¡
     */
    fun logBannerLoadComplete(isActivityFinishing: Boolean = false) {
        bannerLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = bannerLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (bannerLoadRequestTimeMs > 0) {
            bannerLoadCompleteTimeMs - bannerLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "??BANNER LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // ?„ìž¬ Activity ?íƒœ ?•ì¸
        if (isActivityFinishing) {
            Log.w(TAG, "? ï¸ Activity is FINISHING - banner loaded too late!")
        }

        // MainActivity?€ ë¹„êµ
        if (mainActivityCreateTimeMs > 0) {
            val gap = bannerLoadCompleteTimeMs - mainActivityCreateTimeMs
            if (gap > 0) {
                Log.w(TAG, "? ï¸ Banner loaded ${gap}ms AFTER MainActivity created")
                Log.w(TAG, "? ï¸ DIAGNOSIS: Banner missed display opportunity due to late loading")
            } else {
                Log.d(TAG, "??Banner loaded ${-gap}ms BEFORE MainActivity created (good timing)")
            }
        }
    }

    /**
     * AppOpen ê´‘ê³  ë¡œë“œ ?”ì²­ ?œê° ê¸°ë¡
     */
    fun logAppOpenLoadRequest() {
        appOpenLoadRequestTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadRequestTimeMs - appStartTimeMs
        Log.d(TAG, "?? APP OPEN AD LOAD REQUESTED: t+${elapsed}ms")
    }

    /**
     * AppOpen ê´‘ê³  ë¡œë“œ ?„ë£Œ ?œê° ê¸°ë¡
     */
    fun logAppOpenLoadComplete() {
        appOpenLoadCompleteTimeMs = System.currentTimeMillis()
        val elapsed = appOpenLoadCompleteTimeMs - appStartTimeMs
        val loadDuration = if (appOpenLoadRequestTimeMs > 0) {
            appOpenLoadCompleteTimeMs - appOpenLoadRequestTimeMs
        } else 0L

        Log.d(TAG, "??APP OPEN AD LOADED: t+${elapsed}ms (load took ${loadDuration}ms)")

        // SplashScreen???´ë? ì¢…ë£Œ?˜ì—ˆ?”ì? ?•ì¸
        if (splashScreenFinishTimeMs > 0 && appOpenLoadCompleteTimeMs > splashScreenFinishTimeMs) {
            val gap = appOpenLoadCompleteTimeMs - splashScreenFinishTimeMs
            Log.w(TAG, "? ï¸ AppOpen loaded ${gap}ms AFTER SplashScreen finished")
            Log.w(TAG, "? ï¸ DIAGNOSIS: AppOpen ad missed display opportunity")
        }
    }

    /**
     * ìµœì¢… ?€?´ë° ë¦¬í¬??ì¶œë ¥
     */
    fun printTimingReport() {
        Log.d(TAG, "?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•")
        Log.d(TAG, "?“Š AD TIMING DIAGNOSIS REPORT")
        Log.d(TAG, "?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•")

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

        // Banner ?€?´ë° ë¶„ì„
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

        // AppOpen ?€?´ë° ë¶„ì„
        if (appOpenLoadCompleteTimeMs > 0 && splashScreenFinishTimeMs > 0) {
            val gap = splashScreenFinishTimeMs - appOpenLoadCompleteTimeMs
            if (gap < 0) {
                Log.w(TAG, "  ??PROBLEM: AppOpen loaded ${-gap}ms AFTER SplashScreen finished")
                Log.w(TAG, "  ??AppOpen ad missed display opportunity")
            } else {
                Log.d(TAG, "  ??OK: AppOpen loaded ${gap}ms BEFORE SplashScreen finished")
            }
        }

        Log.d(TAG, "?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•?â•")
    }

    /**
     * ?µê³„ ë¦¬ì…‹ (?ŒìŠ¤?¸ìš©)
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

