package kr.sweetapps.alcoholictimer.util.serialization

import kotlinx.serialization.json.Json

/**
 * [ROBUST] 안전한 JSON 직렬화 설정
 *
 * 구버전 데이터와 신버전 코드 간의 충돌 방지를 위한 설정:
 * 1. ignoreUnknownKeys = true: 코드에 없는 필드가 JSON에 있어도 무시
 * 2. coerceInputValues = true: null이 들어올 수 없는 필드에 null이 오면 기본값 사용
 * 3. isLenient = true: 엄격하지 않은 JSON 포맷 허용
 * 4. encodeDefaults = false: 기본값은 JSON에 포함하지 않음 (크기 최적화)
 */
object JsonConfig {
    val json = Json {
        // [핵심 1] 알 수 없는 필드 무시 (삭제된 기능 대응)
        ignoreUnknownKeys = true

        // [핵심 2] null이 올 수 없는 필드에 null이 오면 기본값 사용
        coerceInputValues = true

        // [핵심 3] 엄격하지 않은 JSON 포맷 허용 (따옴표 누락 등)
        isLenient = true

        // 기본값은 JSON에 포함하지 않음 (크기 최적화)
        encodeDefaults = false

        // Pretty print는 디버그 시에만 (릴리즈에서는 false 권장)
        prettyPrint = false
    }
}

