package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.model.MotivationalQuotes

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
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 장식용 아이콘 (Visual Anchor)
        Text(
            text = "❝",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 40.sp,
                lineHeight = 24.sp, // 라인 높이를 더 줄임
                fontFamily = FontFamily.Serif
            ),
            color = Color(0xFFBDBDBD),
            modifier = Modifier.offset(y = 8.dp) // 아래로 밀어서 텍스트에 붙임
        )

        // 2. 명언 텍스트
        Text(
            text = displayQuote,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF4A4A4A),
                fontWeight = FontWeight.Medium
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