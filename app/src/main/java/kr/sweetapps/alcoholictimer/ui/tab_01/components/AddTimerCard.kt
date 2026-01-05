// [NEW] Refactored from RunScreen.kt (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.sweetapps.alcoholictimer.R

/**
 * [NEW] 타이머 추가 카드 UI (2026-01-05)
 * [REFACTORED] RunScreen.kt에서 분리 (2026-01-05)
 */
@Composable
fun AddTimerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bigCardHeightPx = with(density) { 260.dp.toPx() }
    val bigCardHeight = with(density) { (bigCardHeightPx / density.density).dp }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(bigCardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5) // 연한 회색
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, Color(0xFFE0E0E0))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // [ICON] 큰 + 아이콘
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Timer",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // [TEXT] 안내 문구
                Text(
                    text = stringResource(R.string.add_new_timer_message),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

