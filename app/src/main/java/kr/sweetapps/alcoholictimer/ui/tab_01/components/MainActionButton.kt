// [NEW] Core UI 리팩토링: MainActionButton을 tab_01/components로 이동
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kr.sweetapps.alcoholictimer.R

/**
 * [FIXED_SIZE] 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 Modifier 생성
 *
 * 사용자가 핸드폰 설정에서 "화면 확대" 또는 "글꼴 크기"를 변경해도
 * 버튼의 물리적 크기는 항상 동일하게 유지됩니다.
 */
@Composable
private fun Modifier.fixedSize(dp: Dp): Modifier {
    val density = LocalDensity.current
    val pixels = with(density) { dp.toPx() }
    return this.requiredSize(with(density) { (pixels / density.density).dp })
}

@Composable
fun MainActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColorRes: Int = R.color.color_progress_primary,
    size: Dp = 96.dp,
    iconSize: Dp = 48.dp,
    elevationDp: Dp = 8.dp, // passed to CardDefaults
    icon: ImageVector = Icons.Default.PlayArrow,
    contentDescription: String? = null
) {
    // [FIXED_SIZE] 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 적용
    val base = Modifier.fixedSize(size)

    Card(
        onClick = { if (enabled) onClick() },
        modifier = base.then(modifier),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = colorResource(id = containerColorRes)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationDp)
    ) {
        Box(modifier = base, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fixedSize(iconSize)
            )
        }
    }
}
