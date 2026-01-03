@file:Suppress("UNUSED_PARAMETER")
package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP 통합 구현체 (Window Focus 문제 완벽 해결 버전)
 */
class UmpConsentManager(private val context: Context) {
    private val TAG = "UmpConsentManager"

    // 동시 실행 방지 플래그
    private val isGathering = AtomicBoolean(false)

    @Volatile
    var canRequestAds: Boolean = false
        private set

    /** 현재 UMP 폼이 표시 중인지 여부 */
    @Volatile
    private var formShowing: Boolean = false

    fun isFormShowing(): Boolean = formShowing

    /**
     * [FIX v12] UMP 동의 수집 - 방어적 패턴 (2026-01-03)
     *
     * 해결한 문제:
     * 1. 화면 겹침: UMP 폼과 알림 팝업 동시 표시
     *    → 해결: requestConsentInfoUpdate 성공 시 타임아웃 즉시 해제
     * 2. 좀비 폼: 타임아웃 후 뒤늦게 UMP 폼 표시
     *    → 해결: isFinished 체크로 Late Show 완전 차단
     * 3. 앱 멈춤: UMP 응답 없을 때 무한 대기
     *    → 해결: 4초 타임아웃 유지
     *
     * 핵심 전략:
     * - AtomicBoolean으로 중복 실행 차단
     * - 타임아웃은 '정보 업데이트'만 대상 (폼 표시는 대상 아님)
     * - 좀비 폼 방지 체크 추가
     */
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (isGathering.getAndSet(true)) {
            Log.w(TAG, "gatherConsent() ignored: already in progress")
            return
        }

        Log.d(TAG, "🚀 gatherConsent() start - Safe Sequential Pattern")

        // [1] 중복 실행 완전 차단 플래그
        val isFinished = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        // [2] 타임아웃 Runnable (4초 안전장치)
        var timeoutRunnable: Runnable? = null

        // [3] 앱 진입 함수 (딱 한 번만 실행됨)
        fun proceed() {
            if (isFinished.compareAndSet(false, true)) {
                Log.d(TAG, "✅ Consent flow finished. Proceeding to app...")

                // 타이머 해제 (중요: proceed가 호출될 때마다 확실히 제거)
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                // 상태 정리
                formShowing = false
                isGathering.set(false)

                // UI 스레드에서 콜백 실행
                activity.runOnUiThread {
                    Log.d(TAG, "🎯 Calling onComplete (UI Thread, canRequestAds=$canRequestAds)")
                    onComplete(canRequestAds)
                }
            }
        }

        // [4] 4초 타임아웃 설치 (앱 멈춤 방지)
        timeoutRunnable = Runnable {
            Log.e(TAG, "⏰ FORCE TIMEOUT (4s)! UMP too slow. Proceeding without consent.")
            canRequestAds = false
            proceed()
        }
        mainHandler.postDelayed(timeoutRunnable!!, 4000L)

        // [5] UMP 파라미터 생성
        val params = createConsentRequestParameters(activity)
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        // [6] UMP 로직 시작 (방어적 패턴)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            { // ===== 성공 시 =====
                Log.d(TAG, "📋 Consent Info Available")

                // [방어 1] 성공했으므로 타임아웃 즉시 해제 (화면 겹침 방지)
                // 이유: 정보 업데이트 성공 후 폼 표시 시간은 타임아웃 대상이 아님
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                Log.d(TAG, "   ⏰ Timeout cancelled - consent info update succeeded")

                // [방어 2] 이미 타임아웃으로 진행되었다면 폼을 띄우지 말고 중단 (좀비 폼 방지)
                if (isFinished.get()) {
                    Log.w(TAG, "⚠️ Consent info updated too late (timeout already fired). Skipping form.")
                    return@requestConsentInfoUpdate
                }

                // [방어 3] 안전하므로 폼 표시 시도
                formShowing = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAdError: FormError? ->
                    // ===== UMP 폼이 완전히 닫힌 후 실행되는 콜백 =====
                    formShowing = false

                    if (loadAdError != null) {
                        Log.w(TAG, "⚠️ Form load error: ${loadAdError.message}")
                    }

                    // 동의 상태 최종 확인
                    val finalStatus = consentInfo.consentStatus
                    canRequestAds = finalStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                                   finalStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED

                    Log.d(TAG, "✅ Consent status: $finalStatus, canRequestAds=$canRequestAds")

                    // ★ 여기서만 proceed() 호출! (폼이 완전히 닫힌 후)
                    proceed()
                }
            },
            { error: FormError? -> // ===== 실패 시 =====
                Log.w(TAG, "❌ Consent Info Update Failed: ${error?.message}")
                canRequestAds = false

                // 실패 시 즉시 진행
                proceed()
            }
        )
    }

    private fun createConsentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

        // [FIX v9] Debug 모드면 무조건 EEA 지역 설정 (2026-01-03)
        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            Log.d(TAG, "🇪🇺 Debug 모드 - 강제 EEA 지역 설정")

            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // 🇪🇺 강제 유럽

            // BuildConfig에 등록된 테스트 기기 해시가 있으면 추가
            val testHash = try {
                kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH
            } catch (_: Exception) {
                ""
            }

            if (testHash.isNotBlank()) {
                val testDeviceHashes = testHash.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                testDeviceHashes.forEach { hash ->
                    debugSettingsBuilder.addTestDeviceHashedId(hash)
                    Log.d(TAG, "   ✓ 테스트 기기 해시 추가: $hash")
                }
            } else {
                Log.d(TAG, "   ℹ️ UMP_TEST_DEVICE_HASH 없음 - EEA 설정만 적용")
            }

            // ★ 중요: 실제 기기에서 테스트 중이라면, Logcat에 뜨는 본인의 기기 ID를 여기에 추가하세요
            // 예: debugSettingsBuilder.addTestDeviceHashedId("YOUR_DEVICE_HASH_FROM_LOGCAT")

            builder.setConsentDebugSettings(debugSettingsBuilder.build())
            Log.d(TAG, "   ✅ Debug 설정 완료: EEA 지역 강제 적용")
        }

        return builder.build()
    }

    // ... (resetConsent, showPrivacyOptionsForm 등 나머지는 기존 유지) ...
    fun resetConsent(context: Context) {
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) return
        try {
            UserMessagingPlatform.getConsentInformation(context).reset()
            isGathering.set(false)
            canRequestAds = false
        } catch (t: Throwable) {}
    }

    fun showPrivacyOptionsForm(activity: Activity, onClosed: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onClosed)
    }

    fun isPrivacyOptionsRequired(): Boolean {
        return try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(context)
            consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) { false }
    }
}
