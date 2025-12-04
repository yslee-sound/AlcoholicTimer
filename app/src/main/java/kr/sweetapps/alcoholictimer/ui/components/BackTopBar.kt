package kr.sweetapps.alcoholictimer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider

/**
 * 공통 탑바: 왼쪽 백 아이콘 영역(고정 터치 크기)과 타이틀의 시작 패딩을 UiConstants로 통일합니다.
 * trailingContent는 우측 액션을 넣을 수 있도록 옵션으로 둡니다.
 */
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(MaterialTheme.colorScheme.surface)
    ) {
        val noRipple = remember { MutableInteractionSource() }
        // Make the whole touch area clickable (not just the icon) to improve hit target
        Box(modifier = Modifier
            .align(Alignment.CenterStart)
            .width(UiConstants.BackIconTouchArea)
            .padding(start = UiConstants.BackIconInnerPadding)
            .clickable(indication = null, interactionSource = noRipple) { onBack() },
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_caret_left),
                contentDescription = stringResource(id = R.string.cd_navigate_back),
                modifier = Modifier.size(24.dp)
            )
        }

        // Force the title Text to render with system font scale (1.0) so surrounding
        // CompositionLocalProvider fontScale changes do not affect top-bar typography.
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            androidx.compose.material3.Text(
                text = title,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = UiConstants.BackIconStartPadding)
            )
        }

        if (trailingContent != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                trailingContent()
            }
        }
    }
}
