package kr.sweetapps.alcoholictimer.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 자동 크기 조절 텍스트 (RunScreen 방식)
 * 렌더링 전에 최적 크기를 계산하여 깜빡임 현상 없음
 */
@Composable
fun AutoResizingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current,
    minFontSize: TextUnit = 10.sp,
    maxWidthFraction: Float = 0.92f
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val initialFontSize = if (fontSize != TextUnit.Unspecified) {
        fontSize
    } else {
        style.fontSize
    }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        // 렌더링 전에 모든 계산 완료 (깜빡임 방지)
        val optimalFontSize = remember(text, maxWidthPx) {
            var currentSize = initialFontSize.value
            val minSize = minFontSize.value

            while (currentSize >= minSize) {
                val testStyle = style.copy(
                    fontSize = currentSize.sp,
                    fontWeight = fontWeight,
                    color = color
                )

                val result = try {
                    textMeasurer.measure(
                        text = AnnotatedString(text),
                        style = testStyle
                    )
                } catch (_: Throwable) {
                    null
                }

                val textWidth = result?.size?.width ?: 0
                if (textWidth <= maxWidthPx * maxWidthFraction) {
                    break
                }
                currentSize -= 0.5f
            }
            currentSize.coerceAtLeast(minSize)
        }

        // 계산된 크기로 단 한 번만 렌더링
        Text(
            text = text,
            color = color,
            fontSize = optimalFontSize.sp,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = style.copy(fontSize = TextUnit.Unspecified)
        )
    }
}

