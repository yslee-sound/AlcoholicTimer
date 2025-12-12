package kr.sweetapps.alcoholictimer.data.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 금주 타이머 앱 - 동기부여 명언 데이터 (다국어 지원)
 */
object MotivationalQuotes {

    // [NEW] 중복 방지용 '카드 덱' - 컨텍스트별로 관리
    private val currentDeck = ArrayList<String>()

    /**
     * 중복 없는 랜덤 명언 반환 (Deck 알고리즘)
     * - strings.xml에서 명언을 가져와 다국어 지원
     * - 모든 명언을 다 보여주기 전까진 절대 겹치지 않음
     */
    fun getRandomQuote(context: Context): String {
        if (currentDeck.isEmpty()) {
            // strings.xml에서 명언 배열 가져오기
            val quotes = context.resources.getStringArray(
                context.resources.getIdentifier(
                    "motivational_quotes",
                    "array",
                    context.packageName
                )
            )

            // 덱이 비었으면 다시 채우고 섞음 (무한 리필)
            currentDeck.addAll(quotes.toList().shuffled())
        }
        // 카드 한 장 뽑기 (꺼내서 보여주고 덱에서 삭제)
        return currentDeck.removeAt(0)
    }
}

/**
 * Composable에서 사용할 수 있는 헬퍼 함수
 */
@Composable
fun getRandomMotivationalQuote(): String {
    val context = LocalContext.current
    return MotivationalQuotes.getRandomQuote(context)
}
