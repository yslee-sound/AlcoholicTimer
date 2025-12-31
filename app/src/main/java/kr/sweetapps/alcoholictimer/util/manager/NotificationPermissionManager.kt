package kr.sweetapps.alcoholictimer.util.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * 알림 권한 관리 클래스
 *
 * Android 13+ (API 33+)의 POST_NOTIFICATIONS 권한 요청 처리
 * ActivityResultLauncher 방식 사용
 *
 * @since 2025-12-31
 */
object NotificationPermissionManager {

    /**
     * 알림 권한이 필요한 Android 버전인지 확인
     * @return true: Android 13+ (권한 필요), false: Android 12 이하 (권한 불필요)
     */
    fun isPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 현재 알림 권한이 허용되어 있는지 확인
     * @return true: 권한 허용됨, false: 권한 거부됨 또는 미요청
     */
    fun hasPermission(context: Context): Boolean {
        return if (!isPermissionRequired()) {
            // Android 12 이하는 자동으로 권한 있음
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 권한 요청이 필요한지 확인
     * @return true: 권한 요청 필요, false: 권한 요청 불필요 (이미 허용됨 또는 Android 12 이하)
     */
    fun shouldRequestPermission(context: Context): Boolean {
        return isPermissionRequired() && !hasPermission(context)
    }

    /**
     * ActivityResultLauncher를 사용하여 시스템 권한 팝업 표시
     * (Pre-Permission 다이얼로그에서 '확인' 버튼 클릭 시 호출)
     *
     * @param launcher ActivityResultLauncher 인스턴스
     */
    fun requestPermission(launcher: ActivityResultLauncher<String>) {
        if (!isPermissionRequired()) {
            android.util.Log.d("NotificationPermission", "Android 12 이하 - 권한 요청 불필요")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                android.util.Log.d("NotificationPermission", "🔔 시스템 권한 팝업 요청 (ActivityResultLauncher)")
            } catch (e: Exception) {
                android.util.Log.e("NotificationPermission", "권한 요청 실패", e)
            }
        }
    }
}

