package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.model.MotivationalQuotes

/**
 * [SHARED] 명언/응원 문구 공통 스타일 정의
 */
object QuoteTextStyle {
    val default = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        textAlign = TextAlign.Center,
        color = Color(0xFF5A5A5A),
        fontWeight = FontWeight.Medium
    )

    val quoteMarkColor = Color(0xFFBDBDBD)
}

/**
 * [Updated] 스마트 명언 디스플레이 (자동 리사이징 + 다국어 지원)
 *
 * 이 컴포넌트는 내부적으로 AutoResize 로직을 포함하고 있어,
 * 글자 수가 많거나 언어가 달라도 항상 지정된 줄 수(기본 2줄) 안에 맞춰집니다.
 */
@Composable
fun QuoteDisplay(
    modifier: Modifier = Modifier,
    quote: String? = null
) {
    val context = LocalContext.current

    // [FIX] 1. 현재 시스템 언어 설정을 감지합니다.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val currentLanguage = configuration.locales[0].language // 예: "ko", "en"

    // [FIX] 2. rememberSaveable에 'currentLanguage'를 키(Key)로 전달합니다.
    // 의미: "언어가 바뀌면(키가 변하면), 기억하던 명언을 버리고 새로 가져와!"
    val displayQuote = rememberSaveable(inputs = arrayOf(currentLanguage)) {
        quote ?: MotivationalQuotes.getRandomQuote(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp), // [MODIFIED] 10dp → 6dp 여백 축소 (2025-12-24)
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... (나머지 코드는 그대로)
        // 1. 장식용 아이콘 (따옴표)
        Text(
            text = "❝",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 32.sp,
                lineHeight = 24.sp, // [MODIFIED] 줄 높이를 fontSize보다 작게 설정 (2025-12-24)
                fontFamily = FontFamily.Serif
            ),
            color = QuoteTextStyle.quoteMarkColor,
            modifier = Modifier.offset(y = (-4).dp) // [NEW] 따옴표를 위로 약간 이동 (2025-12-24)
        )

        // [NEW] 따옴표와 명언 사이 간격 조절 (2025-12-24)
        Spacer(modifier = Modifier.height((-8).dp))

        // 2. 명언 텍스트 - [핵심 변경] 단순 Text 대신 자동 리사이징 적용
        AutoResizeMultiLineText(
            text = displayQuote,
            baseStyle = QuoteTextStyle.default,
            maxLines = 2, // ★ 여기서 2줄 강제
            minFontSizeSp = 11f, // 최소 11sp까지만 축소 (너무 작아지지 않게 방어)
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * [PRIVATE Logic] 텍스트 자동 크기 조절 로직
 * RunScreen에 있던 로직을 공통 컴포넌트 내부로 캡슐화했습니다.
 * 외부에서는 이 복잡한 로직을 알 필요 없이 QuoteDisplay만 쓰면 됩니다.
 */
@Composable
private fun AutoResizeMultiLineText(
    text: String,
    baseStyle: TextStyle,
    maxLines: Int = 2,
    modifier: Modifier = Modifier,
    minFontSizeSp: Float = 10f,
    textAlign: TextAlign = TextAlign.Center
) {
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val containerWidth = with(density) { maxWidth.toPx() }

        // [핵심] 렌더링 전 사전 계산 (Pre-calculation)
        // 다국어(영어, 한국어 등) 길이 차이를 측정하여 폰트 크기 결정
        val resultStyle = remember(text, containerWidth, baseStyle, maxLines) {
            var currentSize = baseStyle.fontSize.value
            var bestStyle = baseStyle

            // 폰트 크기를 줄여가며 maxLines 안에 들어가는지 확인
            while (currentSize >= minFontSizeSp) {
                val proposedStyle = baseStyle.copy(
                    fontSize = currentSize.sp,
                    lineHeight = (currentSize * 1.35f).sp // 글자가 작아지면 줄간격도 같이 줄임
                )

                val result = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = proposedStyle,
                    constraints = Constraints(maxWidth = containerWidth.toInt())
                )

                // maxLines 이하이고 가로로 넘치지 않으면 채택
                if (result.lineCount <= maxLines && !result.hasVisualOverflow) {
                    bestStyle = proposedStyle
                    break
                }

                currentSize -= 0.5f // 0.5sp씩 정밀하게 줄임
            }
            bestStyle.copy(fontSize = currentSize.coerceAtLeast(minFontSizeSp).sp)
        }

        Text(
            text = text,
            style = resultStyle,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis, // 최후의 수단 (말줄임표)
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEDE9)
@Composable
private fun QuoteDisplayPreview() {
    MaterialTheme {
        Column {
            // 짧은 문구 테스트
            QuoteDisplay(quote = "Short quote.")

            // 긴 문구 테스트 (자동 축소 확인)
            QuoteDisplay(quote = "Don't sacrifice tomorrow's condition for momentary pleasure right now.")
        }
    }
}