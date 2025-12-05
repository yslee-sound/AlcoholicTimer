package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.model.MotivationalQuotes

/**
 * [SHARED] 명언/응원 문구 공통 스타일 정의
 *
 * 시작 화면과 런 화면에서 공통으로 사용하는 명언 텍스트 스타일입니다.
 * 일관된 UX를 위해 한 곳에서 관리합니다.
 */
object QuoteTextStyle {
    /**
     * 명언 텍스트 스타일
     */
    val default = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        lineHeight = 28.sp, // 줄 간격
        textAlign = TextAlign.Center,
        color = Color(0xFF5A5A5A),
        fontWeight = FontWeight.Medium
    )

    /**
     * 장식용 따옴표 색상
     */
    val quoteMarkColor = Color(0xFFBDBDBD)
}

/**
 * [Updated] 심리스 디자인 명언 컴포넌트
 *
 * 카드 테두리를 제거하고 배경에 자연스럽게 녹아드는 디자인을 적용했습니다.
 * 상단에 장식용 아이콘(따옴표)을 배치하여 시각적 중심을 잡습니다.
 */
@Composable
fun QuoteDisplay(
    modifier: Modifier = Modifier,
    quote: String? = null
) {
    // 문구 랜덤 선택 및 유지
    val displayQuote = rememberSaveable {
        quote ?: MotivationalQuotes.getRandomQuote()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 장식용 아이콘 (Visual Anchor)
        Text(
            text = "❝",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 40.sp,
                lineHeight = 20.sp, // 라인 높이를 최소화
                fontFamily = FontFamily.Serif
            ),
            color = QuoteTextStyle.quoteMarkColor
        )

        // 2. 명언 텍스트 - [SHARED STYLE]
        Text(
            text = displayQuote,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = QuoteTextStyle.default.fontFamily,
                fontSize = QuoteTextStyle.default.fontSize,
                lineHeight = QuoteTextStyle.default.lineHeight,
                textAlign = QuoteTextStyle.default.textAlign,
                color = QuoteTextStyle.default.color,
                fontWeight = QuoteTextStyle.default.fontWeight
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEDE9)
@Composable
private fun QuoteDisplayPreview() {
    MaterialTheme {
        QuoteDisplay()
    }
}