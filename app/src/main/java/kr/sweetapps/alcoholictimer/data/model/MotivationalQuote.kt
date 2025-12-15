package kr.sweetapps.alcoholictimer.data.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.R // [중요] R 리소스 import 확인

/**
 * 금주 타이머 앱 - 동기부여 명언 데이터 (다국어 지원)
 */
object MotivationalQuotes {

    private val currentDeck = ArrayList<String>()
    private var currentLanguage: String? = null

    /**
     * 중복 없는 랜덤 명언 반환
     */
    fun getRandomQuote(context: Context): String {
        // 1. 현재 시스템 언어 감지
        val configuration = context.resources.configuration
        val currentLocale = configuration.locales[0].language // "ko", "en", "ja" ...

        // 2. 언어가 바뀌었으면 덱 초기화 (새 언어로 다시 뽑기 위해)
        if (currentLanguage != currentLocale) {
            currentDeck.clear()
            currentLanguage = currentLocale
        }

        // 3. 덱이 비었으면 리필
        if (currentDeck.isEmpty()) {
            try {
                // [FIX] getIdentifier 대신 R.array 직접 참조 (훨씬 빠르고 안전함)
                // 주의: 각 언어별 strings.xml에 <string-array name="motivational_quotes">가 없으면
                // 자동으로 기본(영어) 값을 가져옵니다.
                val quotes = context.resources.getStringArray(R.array.motivational_quotes)
                currentDeck.addAll(quotes.toList().shuffled())
            } catch (e: Exception) {
                // 예외 발생 시 기본 문구 반환 (앱 죽음 방지)
                return "You can do it!"
            }
        }

        if (currentDeck.isEmpty()) return "Stay Strong!"

        return currentDeck.removeAt(0)
    }
}

@Composable
fun getRandomMotivationalQuote(): String {
    val context = LocalContext.current
    return MotivationalQuotes.getRandomQuote(context)
}