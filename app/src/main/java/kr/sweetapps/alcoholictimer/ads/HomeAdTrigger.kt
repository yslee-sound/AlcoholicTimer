package kr.sweetapps.alcoholictimer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * 홈(시작) 화면 재진입 횟수를 기반으로 전면 광고를 완화하여 노출하는 트리거.
 * 기본 정책: 홈 화면이 실제로 사용자에게 보여진(자동 Run 이동이 발생하지 않은) 회수가 threshold에 도달하면 전면 광고 1회 시도.
 *
 * 초기 경험 보호: 앱 시작 후 일정 시간(기본 5분) 동안은 광고를 표시하지 않음.
 */
object HomeAdTrigger {
    private const val PREFS_NAME = "home_ad_trigger_prefs"
    private const val KEY_HOME_VISITS = "home_visits_count"
    private const val KEY_LAST_RESET_DAY = "home_visits_day"
    private const val KEY_APP_START_TIME = "app_start_time_ms"

    // 3회 방문 시도 (Pocket Chord 유사 패턴)
    private const val VISIT_THRESHOLD = 3

    // 앱 시작 후 쿨다운 시간 (밀리초): 5분
    private const val INITIAL_COOLDOWN_MS = 5 * 60 * 1000L

    // 일일 초기화 위한 포맷
    private fun dayKey(): String = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 앱 시작 시각을 기록 (MainApplication 또는 첫 Activity onCreate에서 호출)
     */
    fun recordAppStart(context: Context) {
        prefs(context).edit {
            putLong(KEY_APP_START_TIME, System.currentTimeMillis())
        }
        Log.d("HomeAdTrigger", "App start time recorded")
    }

    /**
     * 초기 쿨다운 기간 내인지 확인
     */
    private fun isInInitialCooldown(context: Context): Boolean {
        val startTime = prefs(context).getLong(KEY_APP_START_TIME, 0L)
        if (startTime == 0L) {
            // 기록이 없으면 지금 기록하고 쿨다운 적용
            recordAppStart(context)
            return true
        }
        val elapsed = System.currentTimeMillis() - startTime
        return elapsed < INITIAL_COOLDOWN_MS
    }

    /** 홈 화면이 실제로 표시되었을 때 호출. */
    fun registerHomeVisit(activity: Activity) {
        if (!AdController.isInterstitialEnabled()) {
            // 정책상 비활성화면 카운트만 초기화
            resetIfDayChanged(activity)
            return
        }

        // 초기 쿨다운 체크: 앱 시작 후 5분 이내면 광고 시도 안 함
        if (isInInitialCooldown(activity)) {
            val startTime = prefs(activity).getLong(KEY_APP_START_TIME, 0L)
            val remaining = INITIAL_COOLDOWN_MS - (System.currentTimeMillis() - startTime)
            val remainingSec = (remaining / 1000).coerceAtLeast(0)
            Log.d("HomeAdTrigger", "Initial cooldown active: ${remainingSec}s remaining")
            // 프리로드만 수행하고 카운트는 증가시키지 않음
            if (!InterstitialAdManager.isLoaded()) {
                InterstitialAdManager.preload(activity.applicationContext)
            }
            return
        }

        resetIfDayChanged(activity)
        val sp = prefs(activity)
        val current = sp.getInt(KEY_HOME_VISITS, 0) + 1
        sp.edit {
            putInt(KEY_HOME_VISITS, current)
        }
        Log.d("HomeAdTrigger", "Home visit recorded: $current/$VISIT_THRESHOLD")
        if (current >= VISIT_THRESHOLD) {
            sp.edit {
                putInt(KEY_HOME_VISITS, 0)
            }
            // 광고 시도 (프리로드 상태 고려). fallback은 없음.
            AdHelpers.preloadThenShowOr(activity, timeoutMs = 1200) {
                // 실패하거나 조건 미충족 시 다음 기회 대비 프리로드만 수행
                InterstitialAdManager.preload(activity.applicationContext)
            }
        } else {
            // 아직 임계치 전 => 조용히 프리로드 유지 (없으면 로드)
            if (!InterstitialAdManager.isLoaded()) {
                InterstitialAdManager.preload(activity.applicationContext)
            }
        }
    }

    private fun resetIfDayChanged(context: Context) {
        val sp = prefs(context)
        val lastDay = sp.getString(KEY_LAST_RESET_DAY, null)
        val today = dayKey()
        if (lastDay != today) {
            sp.edit {
                putString(KEY_LAST_RESET_DAY, today)
                putInt(KEY_HOME_VISITS, 0)
            }
        }
    }
}

