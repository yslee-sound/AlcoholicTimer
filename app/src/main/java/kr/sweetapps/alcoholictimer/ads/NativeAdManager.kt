package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.util.Log

/**
 * 네이티브 광고 로더 및 정책 관리 싱글톤
 * - 디버그: 정책 우회, 항상 허용
 * - 릴리즈: 일일 캡(1-2회), 쿨다운(2분), UMP 동의 게이팅
 */
object NativeAdManager {
    private const val TAG = "NativeAdManager"

    // 네이티브 광고 기능 전체 비활성화
    fun preload(context: Context) {
        Log.d(TAG, "NativeAdManager: 네이티브 광고 기능 비활성화됨")
    }

    /**
     * 광고가 로드되어 있는지 확인
     */
    fun isLoaded(): Boolean = false

    /**
     * 정책 체크: 광고 표시가 허용되는지 확인
     * @return true if allowed, false otherwise
     */
    fun canShowAd(context: Context): Boolean = false

    /**
     * 광고 표시 후 호출 (정책 카운터 업데이트)
     */
    fun recordShown(context: Context) {
        Log.d(TAG, "NativeAdManager: 네이티브 광고 기능 비활성화됨")
    }
}

