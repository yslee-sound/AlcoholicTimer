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
import kr.sweetapps.alcoholictimer.R

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
    // enforce base size first so external modifiers won't shrink or stretch the button unexpectedly
    // requiredSize forces both width and height to the given value regardless of parent constraints
    val base = Modifier.requiredSize(size)

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
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
