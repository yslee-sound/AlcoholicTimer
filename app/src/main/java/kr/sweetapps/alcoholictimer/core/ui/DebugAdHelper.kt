package kr.sweetapps.alcoholictimer.core.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Debug 빌드에서만 배너 숨김 토글을 제공하던 헬퍼입니다.
 * 현재 앱 UI에서 디버그 메뉴를 제거했으므로 더 이상 기본 경로에서 사용되지 않습니다.
 * 필요시 debug 빌드에서만 다시 활성화할 수 있습니다.
 */
@Deprecated("디버그 메뉴 제거로 인해 기본 사용 경로에서 사용되지 않음")
object DebugAdHelper {
    // 디버그 전용 토글 상태. 기본 false(표시)
    private val _bannerHiddenFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val bannerHiddenFlow: StateFlow<Boolean> = _bannerHiddenFlow

    fun initialize(context: Context) {
        // 확장 여지: SharedPreferences 로딩, 개발자 옵션 등의 동기화
        // 현재는 no-op. 디버그에서만 값 수정을 허용하도록 유지.
    }

    // 테스트/디버그용 토글 API (debug 빌드에서만 사용 권장)
    fun setBannerHidden(hidden: Boolean) { _bannerHiddenFlow.value = hidden }
}
