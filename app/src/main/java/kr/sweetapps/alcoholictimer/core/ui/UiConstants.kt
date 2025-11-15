package kr.sweetapps.alcoholictimer.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.core.util.Constants

/**
 * 기존 `UiConstants`는 더 이상 고정값을 담지 않고
 * `core.util.Constants`의 값을 Compose-friendly 형태로 위임합니다.
 * (추후에는 `Constants`에서 관리하는 중앙값만 수정하면 됩니다.)
 */
object UiConstants {
    // Compose에서 직접 사용 가능한 Dp 값으로 변환
    val BackIconStartPadding: Dp = Constants.BACK_ICON_START_PADDING_DP.dp
    // 아이콘 터치 영역 (Dp)
    val BackIconTouchArea: Dp = Constants.BACK_ICON_TOUCH_AREA_DP.dp
}
