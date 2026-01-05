// [NEW] Refactored from RunScreen.kt (2026-01-05)
package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.tab_01.viewmodel.Tab01ViewModel

/**
 * [NEW] 기존 타이머 카드 UI (HorizontalPager용) (2026-01-05)
 * [UPDATED] 페이지별 그라데이션 배경 적용 (2026-01-05)
 * [REFACTORED] RunScreen.kt에서 분리 (2026-01-05)
 */
@Composable
fun TimerCard(
    timerData: Tab01ViewModel.TimerData,
    displayElapsedMillis: Long,
    targetDays: Float,
    elapsedDaysFloat: Float,
    remainingDays: Int,
    progressTimeText: String,
    progress: Float,
    backgroundBrush: Brush,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val bigCardHeightPx = with(density) { 260.dp.toPx() }
    val bigCardHeight = with(density) { (bigCardHeightPx / density.density).dp }

    Card(
        modifier = modifier.fillMaxWidth().requiredHeight(bigCardHeight),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 그라데이션 배경
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = backgroundBrush)
            )

            // 카드 내용
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // [TOP] 경과 일수와 시간
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    val daysValue = String.format(Locale.getDefault(), "%.0f", kotlin.math.floor(elapsedDaysFloat.toDouble()))
                    val daysCount = kotlin.math.floor(elapsedDaysFloat.toDouble()).toInt()
                    val daysUnit = remember(daysCount) {
                        context.resources.getQuantityString(R.plurals.days_count, daysCount, daysCount).substringAfter(" ")
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = daysValue,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 72.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = daysUnit,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                fontSize = 24.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 2f
                                )
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = progressTimeText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 24.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // [BOTTOM] 진행률 바와 퍼센트
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 진행률 바
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x4DFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 퍼센트와 목표 아이콘
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 2f
                                )
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.hourglassmedium),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$remainingDays",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

