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
            .padding(horizontal = 24.dp, vertical = 6.dp), // [OPTIMIZED] 수직 밀도 최적화 (30dp,10dp → 24dp,6dp)
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 장식용 아이콘 (Visual Anchor)
        // 레퍼런스의 원형 아이콘처럼, 시선을 모아주는 역할
        // (아이콘이 없다면 텍스트로 대체 가능: "❝")
        Text(
            text = "❝",
            style = MaterialTheme.typography.displayMedium,
            color = Color(0xFFBDBDBD), // 연한 회색으로 은은하게
            fontSize = 40.sp,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(6.dp)) // [OPTIMIZED] 8dp → 6dp

        // 2. 명언 텍스트
        Text(
            text = displayQuote,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif, // 클래식 명조체
                fontSize = 16.sp,              // 크기 약간 키움
                lineHeight = 26.sp,            // 줄간격 넉넉하게
                textAlign = TextAlign.Center,
                color = Color(0xFF4A4A4A),     // 짙은 회색 (종이에 쓴 느낌)
                fontWeight = FontWeight.Medium
            )
        )

        // 3. (선택사항) 하단 닫는 따옴표 - 너무 장식적이면 제거해도 됨
        /*
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "❞",
            style = MaterialTheme.typography.displayMedium,
            color = Color(0xFFBDBDBD),
            fontSize = 40.sp,
            fontFamily = FontFamily.Serif
        )
        */
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEDE9) // 앱 배경색 적용 Preview
@Composable
private fun QuoteDisplaySeamlessPreview() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        QuoteDisplay(
            quote = "인내의 무게는 몇 그램이지만,\n후회의 무게는 몇 톤이나 됩니다."
        )
    }
}