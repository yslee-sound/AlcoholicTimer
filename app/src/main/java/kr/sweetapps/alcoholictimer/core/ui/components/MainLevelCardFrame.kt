package kr.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.colorResource
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.AppCard
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder

/**
 * 레벨 화면 메인 카드의 "외곽(Frame)"만 담당하는 래퍼.
 * - 내부 콘텐츠는 그대로(contentPadding = 0.dp) 둔다.
 * - shape/elevation/border는 현재 앱의 기본 시각과 동일하게 설정(회귀 방지).
 * - 향후 플랫 정책을 전역 적용할 때 이 파일만 수정하면 된다.
 */
@Composable
fun MainLevelCardFrame(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(
        modifier = modifier,
        elevation = AppElevation.CARD, // 플랫 기본(0dp)
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(0.dp), // 내부 패딩은 콘텐츠에 위임
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Column(content = content)
    }
}
