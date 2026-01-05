// [NEW] Refactored from RunScreen.kt (2026-01-05)
// [NEW] Refactored from RunScreen.kt (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R

/**
 * [NEW] 포기 버튼 (2026-01-05)
 * [REFACTORED] RunScreen.kt에서 분리 (2026-01-05)
 *
 * 시스템 폰트 스케일의 영향을 받지 않는 고정 크기 FloatingActionButton
 */
@Composable
fun StopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonSizePx = with(density) { 77.dp.toPx() }
    val buttonSize = with(density) { (buttonSizePx / density.density).dp }
    val iconSizePx = with(density) { 39.dp.toPx() }
    val iconSize = with(density) { (iconSizePx / density.density).dp }

    FloatingActionButton(
        onClick = onStop,
        modifier = modifier.requiredSize(buttonSize),
        containerColor = colorResource(id = R.color.color_stop_button),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(id = R.string.cd_stop),
            tint = Color.White,
            modifier = Modifier.requiredSize(iconSize)
        )
    }
}

