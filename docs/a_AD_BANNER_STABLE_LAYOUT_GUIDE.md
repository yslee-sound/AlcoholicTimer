# 배너 광고(AdMob) 안정 레이아웃 & 재시도 운영 가이드

> 이 문서는 AlcoholicTimer 에서 확립한 "상단 고정 배너 + 레이아웃 점프 0" 패턴을 다른 앱에서도 그대로 복제할 수 있도록 정리한 실무 지침입니다. (Jetpack Compose / AdMob Anchored Adaptive Banner 기준)

---
## 1. 목표(Design Goals)
| 목표 | 설명 |
|------|------|
| 레이아웃 안정성 | 로딩/비활성/지연 동의 상태에서 콘텐츠가 위아래로 움직이지 않음 (placeholder 고정) |
| 높은 최초 노출율 | 동의 지연/일시 실패 시 제한적 재시도로 정상 노출 가능성 극대화 |
| 단순성 | Remote Config 없이 코드/내부 상수만으로 운용 |
| 안전성 | AdView 수명 명확 관리(destroy), 과도한 요청/루프 방지 |
| 확장 용이 | 후에 정책/RemoteConfig/프리미엄 대응이 필요해지면 최소 수정으로 확장 |

---
## 2. 핵심 결정(Key Decisions)
1. **Placeholder 고정**: `reserveSpaceWhenDisabled = true` 를 기본값으로 채택 → 배너 비활성(정책, 동의 대기)에도 예측 높이 공간 확보.
2. **Adaptive Anchored 사용**: 폭은 기기 전체 width(dp), 높이 예측 실패 시 fallback 50dp.
3. **Shrink 없음(선택)**: 실제 로드된 배너 높이가 예측보다 크더라도 축소하지 않아 잔여 움직임 제거. (필요 시 향후 옵션화 가능)
4. **재시도 정책 (고정)**: 실패 시 최대 3회, 지수형/느린 증가 딜레이 `[2000, 5000, 10000]` ms.
5. **동의(UMP) 딜레이 폴백**: 최대 15초(1초 간격) 폴링 → canRequestAds() 가능 시 즉시 첫 로드.
6. **전역 1회 요청 플래그 제거**: 화면/액티비티 재생성에도 독립적으로 재시도 가능.
7. **자원 해제**: `DisposableEffect` 로 `adView.destroy()` 호출.
8. **로그 전략**: DEBUG 빌드에서만 상세; RELEASE 는 성공/실패 최소 정보.

---
## 3. 코드 구조 요약
```
AdmobBanner.kt
 ├─ AdmobBanner(@Composable)            // 단일 진입점
 │   ├─ Adaptive 높이 예측 predictedHeight
 │   ├─ reserveSpaceWhenDisabled 기본 true
 │   ├─ 상태: loadState, retryCount, hasSuccessfulLoad, realHeight
 │   ├─ AndroidView(AdView) factory + adListener
 │   ├─ 재시도 LaunchedEffect (retryCount)
 │   ├─ 동의 지연 폴백 LaunchedEffect (adViewRef)
 │   └─ destroy 관리 DisposableEffect
 └─ BannerLoadState sealed class (Loading / Success / Failed)
```

---
## 4. 통합(Integration) 단계
1. **의존성 추가**: `play-services-ads`, `user-messaging-platform` 최신 버전 (Gradle catalog or build.gradle).
2. **Application.onCreate**: `MobileAds.initialize`, UMP Consent 수집 (동의 창 표시 후 canRequestAds() 반영).
3. **AdController.isBannerEnabled()**: 초기엔 true 반환 (추후 서버/프리미엄 연동 시 확장).
4. **상단 배치**: `BaseScaffold` 혹은 `Column` 맨 위 Surface/Box 안에 `AdmobBanner()` 호출.
5. **StatusBar Insets**: 상단 상태바 패딩 후 바로 배너 영역을 둬 "시각적 앱 헤더" 역할.
6. **Divider 위치**: 배너 하단 Hairline / Divider 로 콘텐츠와 구분 (선택).

---
## 5. 추천 상수(Recommended Defaults)
| 항목 | 값 | 근거 |
|------|----|------|
| reserveSpaceWhenDisabled | true | 동의 지연 & 정책 비동기 초기 로딩 중 점프 방지 |
| maxRetry | 3 | 첫 시도 실패율 낮은 환경에서 과도하지 않은 상한 |
| retryDelaysMs | 2000, 5000, 10000 | 네트워크/NO_FILL 일시성 고려한 증가 backoff |
| consentFallbackMaxSeconds | 15 | UMP 동의 창 처리 보통 수초 내 완료 |
| fallbackHeightDp | 50 | AdSize.BANNER 기본 높이와 일치 |
| animation | spring(damping 0.85, stiffness 400) | 필요한 경우 확장 시 유연, 과한 튕김 없음 |

---
## 6. 실패/재시도 흐름(Flow)
1. Loading (placeholder) → loadAd()
2. Success → overlay 제거, (realHeight > predictedHeight + 1dp) 이면 부드럽게 확장 (현재 축소는 제거)
3. Failed(최초) → retryCount++, 지연 후 다시 loadAd()
4. 재시도 종료 조건: (a) 성공 (b) retryCount > maxRetry (c) consent timeout (-2) (d) runCatching 예외 지속 → Failed 유지 (UI는 고정)

---
## 7. 상태 전이 다이어그램 (간단)
```
[Loading] --AdLoaded--> [Success]
[Loading] --AdFailed--> [Failed(retry i)] --schedule--> [Loading] ... (≤ maxRetry)
[Loading] --ConsentTimeout--> [Failed(timeout)]
```

---
## 8. Analytics(선택)
| 이벤트 | 조건 | 목적 |
|--------|------|------|
| banner_first_loaded | 첫 Success | 초기 노출 성공률 추적 |
| banner_all_retries_failed | maxRetry 끝까지 실패 & canRequestAds true | 재시도 정책 조정 판단 |
| banner_consent_timeout | consent 15초 초과 | UMP UX 개선 필요성 판단 |

---
## 9. 공통 문제 & 해결(Troubleshooting)
| 증상 | 가능 원인 | 대응 |
|------|-----------|------|
| 배너 항상 로딩 실패 | 잘못된 Unit ID, 네트워크 차단 | 테스트 ID로 교체 후 비교, 로그 cat 확인 |
| NO_FILL 반복 | 광고 재고 부족 (시간/국가) | 재시도 줄이거나 추가 수익 포맷 도입 검토 |
| UI 순간 점프 | reserveSpaceWhenDisabled=false 사용 | true 로 변경 또는 Adaptive 예측 로직 확인 |
| 메모리 누수 의심 | destroy 미호출 | DisposableEffect 내부 destroy 유지 확인 |
| 높이 과다 | 예측 높이와 실측 차이 큼 | (드문 케이스) realHeight 재측정 로그 분석 |

---
## 10. 다른 앱에 복제하기(Checklist)
- [ ] Admob 의존성 / Google Maven 세팅
- [ ] UMP 동의 플로우(최초 Activity) 적용
- [ ] `AdController.isBannerEnabled()` 초간단 true 구현 (필요 시 abstraction)
- [ ] 위 `AdmobBanner.kt` 파일 그대로 복사 (패키지 경로 수정)
- [ ] 상단 Scaffold/Screen 첫 child 로 삽입
- [ ] 테스트: (a) 네트워크 ON/OFF (b) 동의 대기 (c) 강제 실패 로그 (d) 정상 성공
- [ ] 릴리즈 빌드: 과도한 DEBUG 로그 제거 또는 등급 낮춤

---
## 11. 향후 확장 포인트
| 기능 | 추가 시 변화 | 난이도 |
|------|--------------|--------|
| Remote Config 재시도 | 재시도 상수 로딩 시점 정책 치환 | ★★☆☆☆ |
| 프리미엄(광고 제거) | isBannerEnabled = false + reserveSpaceWhenDisabled=false | ★★☆☆☆ |
| 다중 배치(상/하) | 재사용: position 파라미터 추가 | ★★☆☆☆ |
| Skeleton/Shimmer | Loading overlay 교체 | ★☆☆☆☆ |
| NO_FILL 지능형 억제 | 실패 코드 기반 재시도 축소 | ★★☆☆☆ |
| 배너 높이 Cache | 회전/재구성 감소 (지금은 필요 낮음) | ★★☆☆☆ |

---
## 12. 보안 & 정책 주의
- 테스트 ID → 실제 배포 전 실제 Unit ID *반드시* 교체.
- UMP 동의 없이 loadAd 금지(canRequestAds == true 조건 준수).
- Placeholder 에 광고 형태를 모방한 클릭 유도 UI 배치 금지 (정책 위반 위험).

---
## 13. 간단 적용 코드 스니펫 (요약)
```kotlin
@Composable
fun AppRoot() {
    Column(Modifier.fillMaxSize()) {
        AdmobBanner() // 상단 고정
        Divider()
        ScreenContent()
    }
}
```

---
## 14. 빠른 검증 스크립트(수동)
1. 첫 실행: 배너 즉시/재시도 후 노출 (logcat: Banner onAdLoaded)
2. 비행기 모드 → 앱 재실행 → 실패 후 재시도 로그 3회 → 네트워크 켜기 → (기본적으로 새 로드 시도)
3. UMP 동의 화면에서 일부러 지연 → placeholder 높이 유지 확인
4. 홈 이동/복귀(재구성) → 중복 destroy crash 없음 확인

---
## 15. 유지보수 메모(TODO 제안)
```kotlin
// TODO: 필요 시 RemoteConfig 로 maxRetry / retryDelays 치환
// TODO: 향후 프리미엄 도입 시 reserveSpaceWhenDisabled = false + graceful fade 처리 고려
```

---
## 16. 결론
이 패턴은 *"상단 고정 배너 + 레이아웃 점프 제로 + 단순 재시도"*를 달성하며 초기 단계 앱에서 운영 복잡도 없이 안정적인 UX를 제공합니다. 향후 스케일 확장 시 위 확장 포인트만 점진 적용하면 됩니다.

> 문의/확장 필요 시: AdmobBanner.kt 내부 주석 우선 확인 후 변경.

