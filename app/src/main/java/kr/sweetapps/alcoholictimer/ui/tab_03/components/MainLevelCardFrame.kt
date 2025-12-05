package kr.sweetapps.alcoholictimer.ui.tab_03.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.AppCard
import kr.sweetapps.alcoholictimer.ui.theme.AppBorder
import kr.sweetapps.alcoholictimer.ui.theme.AppColors
import kr.sweetapps.alcoholictimer.ui.theme.AppElevation

/**
 * 메인 레벨 카드 프레임
 * 배경 이미지와 오버레이를 포함한 공통 카드 컨테이너
 */
@Composable
fun MainLevelCardFrame(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundRes: Int? = null,
    backgroundAlpha: Float = 1.0f,
    backgroundContentScale: ContentScale = ContentScale.Crop,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardContainerColor = if (backgroundRes != null) Color.Transparent else AppColors.SurfaceOverlaySoft

    AppCard(
        modifier = modifier,
        elevation = AppElevation.CARD_HIGH,
        shape = RoundedCornerShape(16.dp),
        containerColor = cardContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (backgroundRes != null) {
                Image(
                    painter = painterResource(id = backgroundRes),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(1.4f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = backgroundContentScale,
                    alignment = Alignment.TopCenter,
                    alpha = backgroundAlpha
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.88f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.12f)
                            )
                        )
                )
            }

            Column(content = content)
        }
    }
}

