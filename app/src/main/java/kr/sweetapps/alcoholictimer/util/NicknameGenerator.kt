package kr.sweetapps.alcoholictimer.util

import java.util.Locale

/**
 * 다국어 지원 닉네임 생성기 (최적화 버전)
 * - 신규 유저에게 랜덤 닉네임을 자동 생성합니다.
 * - 타겟 언어: 한국어, 일본어, 인도네시아어, 영어 (그 외 모든 언어는 영어 기본값 사용)
 * - 조합 방식: [형용사 + 명사] (인도네시아어만 [명사 + 형용사])
 */
object NicknameGenerator {

    // 1. 한국어 (KO)
    private val adjKo = listOf("상쾌한", "용감한", "참는 중인", "행복한", "단단한", "새벽의", "멋진", "슬기로운", "깨끗한", "활기찬", "조용한", "튼튼한", "자유로운", "따뜻한", "부지런한")
    private val aniKo = listOf("펭귄", "호랑이", "쿼카", "독수리", "거북이", "사자", "고양이", "강아지", "부엉이", "돌고래", "사슴", "판다", "여우", "토끼", "곰")

    // 2. 일본어 (JA)
    private val adjJa = listOf("爽やかな", "勇敢な", "幸せな", "頑丈な", "夜明けの", "素敵な", "賢い", "綺麗な", "元気な", "静かな", "丈夫な", "自由な", "暖かい", "勤勉な", "優しい")
    private val aniJa = listOf("ペンギン", "トラ", "クオッカ", "ワシ", "カメ", "ライオン", "猫", "犬", "フクロウ", "イルカ", "シカ", "パンダ", "キツネ", "ウサギ", "クマ")

    // 3. 인도네시아어 (ID/IN) - 어순: 명사 + 형용사
    private val adjId = listOf("Berani", "Kuat", "Bahagia", "Tenang", "Rajin", "Sehat", "Bebas", "Pintar", "Hebat", "Sabar", "Jujur", "Ramah", "Bersih", "Segar", "Bijak")
    private val aniId = listOf("Harimau", "Elang", "Kura-kura", "Kucing", "Singa", "Komodo", "Gajah", "Rusa", "Panda", "Rubah", "Kelinci", "Beruang", "Lumba-lumba", "Burung", "Kancil")

    // 4. 영어 (EN - Default) - 그 외 모든 국가용
    private val adjEn = listOf("Brave", "Happy", "Strong", "Calm", "Cool", "Smart", "Clean", "Fresh", "Silent", "Free", "Wise", "Fast", "Kind", "Warm", "Busy")
    private val aniEn = listOf("Tiger", "Eagle", "Turtle", "Lion", "Cat", "Dog", "Owl", "Dolphin", "Deer", "Panda", "Fox", "Rabbit", "Bear", "Wolf", "Hawk")

    /**
     * 시스템 언어에 맞는 랜덤 닉네임 생성
     * - 한국어, 일본어, 인도네시아어: 해당 언어로 생성
     * - 그 외 모든 언어: 영어로 생성 (스페인어, 중국어, 프랑스어 등 포함)
     */
    fun generateRandomNickname(): String {
        val locale = Locale.getDefault().language.lowercase()

        return when (locale) {
            "ko" -> "${adjKo.random()} ${aniKo.random()}"
            "ja" -> "${adjJa.random()}${aniJa.random()}" // 일본어는 띄어쓰기 없음
            "in", "id" -> "${aniId.random()} ${adjId.random()}" // [중요] 인도네시아어: 명사 + 형용사
            else -> "${adjEn.random()} ${aniEn.random()}" // [Default] 그 외 모든 언어는 영어 사용
        }
    }
}

