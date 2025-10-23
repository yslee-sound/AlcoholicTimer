# 레벨 화면 메인 카드 불변 규칙 및 회귀 가드

목표: 레벨 화면의 메인 카드(CurrentLevelCard) 내부 디테일(텍스트, 구조, 배지, 진행바)을 변형 없이 안정적으로 유지하고, 외곽만 정책적으로 제어 가능하게 만든다.

구현 요약
- 외곽 전용 Frame 도입: `MainLevelCardFrame`
  - path: `app/src/main/java/com/example/alcoholictimer/core/ui/components/MainLevelCardFrame.kt`
  - 책임: elevation/shape/border 등 외곽만 담당. 내부 콘텐츠 padding/레이아웃은 변경 금지.
  - 현재 컨벤션: shape=Rounded 16dp, elevation=`AppElevation.CARD`(2dp), border=1dp(light border)
  - contentPadding=0dp 고정: 내부 컴포저블의 패딩·간격이 바뀌지 않도록 보호.
- 적용: 레벨 메인 카드의 바깥 `Card` → `MainLevelCardFrame`로 교체
  - path: `app/src/main/java/com/example/alcoholictimer/feature/level/LevelActivity.kt`
  - 내부 텍스트/간격/shape(내부 사용)/진행바/색상 토큰은 그대로 유지.

테스트 태그(회귀 가드)
- main_level_card_content, main_level_badge, main_level_title
- main_level_days_row, main_level_days_value, main_level_days_label
- main_level_progress, main_level_progress_fill

회귀 테스트(세만틱스, 라이트 모드)
- path: `app/src/test/java/com/example/alcoholictimer/feature/level/CurrentLevelCardSemanticsTest.kt`
- 검증 포인트:
  - 요소 존재: 배지/제목/일차/진행바 노출
  - 텍스트 정확성: `level.name`, "일차", 숫자 일수(예: 19)
- 환경 고정:
  - 라이트 모드: `AlcoholicTimerTheme(darkTheme = false)`
  - 깜빡임 방지: 테스트에서 `startTime=0L`
  - Robolectric 빌드 지문 NPE 회피: `ShadowBuild.setFingerprint("robolectric")`

왜 스냅샷 대신 세만틱스부터?
- 현재 Paparazzi/Roborazzi 플러그인 조합이 AGP/Gradle 버전과 충돌.
- 목표는 “디테일 변형 회귀 방지”이므로 우선 구조/텍스트 불변을 기계적으로 잡는 세만틱스 테스트 도입.
- 추후 CI/플러그인 정리 후 스냅샷(픽셀) 테스트를 추가해 한 단계 강화 예정.

PR 체크리스트
- [ ] `main_level_*` 테스트 태그 유지 여부 확인(레이아웃 수정 시 필수)
- [ ] `CurrentLevelCardSemanticsTest` 녹색(라이트 모드)
- [ ] 스냅샷 인프라(Paparazzi/Roborazzi) 선택 및 도입 검토. 불가 시 세만틱스 테스트 유지

실행 가이드(Windows cmd)
```
:: 단위 테스트(라이트 모드 세만틱스 포함)
./gradlew.bat :app:testDebugUnitTest

:: 특정 테스트만 실행
./gradlew.bat :app:testDebugUnitTest --tests "*CurrentLevelCardSemanticsTest"

:: 디버그 빌드(옵션)
./gradlew.bat :app:assembleDebug
```

다음 단계(선택)
- Paparazzi 안정 버전 확인 후 플러그인 재도입(또는 Roborazzi) → 라이트 모드 스냅샷 1장 기록/검증 태스크 연결
- 다크 모드 도입 시 동일 테스트/스냅샷을 `darkTheme=true`로 복제(2장 체계 완성)
- 필요 시 `MainLevelCardFrame`에 elevation 플랫 토글(0dp) 추가 → 제품 정책 스위치만으로 외곽 평탄화 적용

