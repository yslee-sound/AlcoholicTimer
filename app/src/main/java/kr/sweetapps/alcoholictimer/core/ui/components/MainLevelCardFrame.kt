package kr.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import kr.sweetapps.alcoholictimer.core.ui.AppColors
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.AppCard
import kr.sweetapps.alcoholictimer.core.ui.AppElevation
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale

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
            // 배경 이미지는 카드 내부 전체를 채우도록 (그대로) 표시
            if (backgroundRes != null) {
                Image(
                    painter = painterResource(id = backgroundRes),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(1.1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = backgroundContentScale,
                    alignment = Alignment.TopCenter,
                    alpha = backgroundAlpha
                )
            }

            Column(content = content)
        }
    }
}
