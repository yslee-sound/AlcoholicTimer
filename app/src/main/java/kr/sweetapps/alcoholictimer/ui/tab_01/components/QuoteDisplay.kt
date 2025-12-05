package kr.sweetapps.alcoholictimer.ui.tab_01.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.data.model.MotivationalQuotes

/**
 * [NEW] 동기부여 명언 표시 컴포넌트
 *
 * 금주 타이머 앱에서 사용자에게 동기부여 메시지를 전달하는 UI 요소입니다.
 * 화면이 처음 로드될 때 랜덤으로 명언을 선택하고, 리컴포지션 시에도 동일한 명언을 유지합니다.
 *
 * @param modifier 레이아웃 수정자
 * @param quote 표시할 명언 (null일 경우 랜덤 선택)
 */
@Composable
fun QuoteDisplay(
    modifier: Modifier = Modifier,
    quote: String? = null
) {
    // [NEW] rememberSaveable로 화면 회전 시에도 동일한 명언 유지
    // quote 파라미터가 제공되지 않으면 랜덤으로 선택
    val displayQuote = rememberSaveable {
        quote ?: MotivationalQuotes.getRandomQuote()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFDF7) // 따뜻한 크림색 배경
        ),
        border = BorderStroke(1.dp, Color(0xFFE8E6E0)), // 은은한 테두리
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayQuote,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif, // 클래식한 명조체 느낌
                    fontSize = 15.sp,
                    lineHeight = 24.sp, // 넉넉한 줄간격으로 가독성 향상
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            )
        }
    }
}

/**
 * [NEW] ViewModel State를 사용하는 버전 (선택적)
 *
 * ViewModel에서 명언을 관리하고 싶을 때 사용합니다.
 *
 * @param modifier 레이아웃 수정자
 * @param quoteState ViewModel에서 제공되는 명언 상태
 */
@Composable
fun QuoteDisplayWithState(
    modifier: Modifier = Modifier,
    quoteState: String
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFDF7)
        ),
        border = BorderStroke(1.dp, Color(0xFFE8E6E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quoteState,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEEEDE9)
@Composable
private fun QuoteDisplayPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuoteDisplay()

        QuoteDisplay(
            quote = "인내의 무게는 몇 그램이지만, 후회의 무게는 몇 톤이나 됩니다."
        )
    }
}

