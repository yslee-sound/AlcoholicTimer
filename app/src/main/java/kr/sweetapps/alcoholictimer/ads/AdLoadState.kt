package kr.sweetapps.alcoholictimer.ads

import java.util.concurrent.atomic.AtomicBoolean

/**
 * 전역 광고 로드 상태를 관리합니다(프로세스 범위)
 * - 임시 패치: 동일 프로세스에서 배너를 여러 번 loadAd 요청하는 것을 차단
 * - 주의: 프로세스 재시작/앱 재설치 시 초기화됩니다.
 */
object AdLoadState {
    val bannerRequested = AtomicBoolean(false)
}

