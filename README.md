# AlcoholicTimer

AlcoholicTimer는 금주(절주) 습관 형성을 돕는 Android 앱입니다.
핵심 기능은 금주 타이머, 진행률/레벨 표시, 기록 관리(목록/상세/전체보기),
간단한 통계 제공입니다. UI는 Jetpack Compose(Material3)로 구현되며,
데이터는 로컬 저장(SharedPreferences/JSON) 방식을 사용합니다.
가벼운 구조와 명확한 흐름을 목표로 하며, 향후 위젯/알림/백업 등
확장 기능을 단계적으로 추가할 예정입니다.

## 빠른 시작 (Windows, Gradle Wrapper)

```bat
REM 클린 & 디버그 빌드
gradlew.bat clean
gradlew.bat :app:assembleDebug

REM Lint 검사
gradlew.bat :app:lintDebug
```

생성된 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 문서
- 전체 기획안: [./docs/APP_SPEC.md](./docs/APP_SPEC.md)
- UX 흐름 명세: [./docs/UX_FLOW.md](./docs/UX_FLOW.md)
- 패키지 리팩터링 실행 계획: [./docs/REFACTORING_PACKAGE_STRUCTURE.md](./docs/REFACTORING_PACKAGE_STRUCTURE.md)

## 모듈 구조
- `app/` — 앱 본체(Activities, Compose UI, utils)
- `docs/` — 제품/UX 사양 문서 및 부속 자료

## 런처 아이콘(Adaptive / Monochrome) 설계 및 변경 내역
| 항목 | 결정 | 근거 |
|------|------|------|
| Adaptive 구조 | 배경 + foreground inset + monochrome 레이어 사용 | Android 8.0+ 가이드 준수, Android 13 테마 아이콘 대응 |
| 안전 영역 | inset 18dp 적용 | 권장 여백(108dp 캔버스 대비 18dp) 확보로 마스크 다양성/가독성 개선 |
| Foreground 스케일 | group scale 1.43 | 이전 1.55 → 과밀 문제 해소, breathing room 확보 |
| 전경 색상 | `@color/icon_launcher_fg` (#C6283A) | 새 배경(#FFFFFF) 대비 명도/채도 대비 확보(약 5.3:1) 및 리소스화로 재사용성 확보 |
| 배경 색상 | `@color/icon_launcher_bg` (#FFFFFF) | 심플/중립적, 브랜드 전경 강조, 다수 런처 배경과 충돌 최소화 |
| Monochrome 아이콘 | 단순화된 hourglass 실루엣(path 1개) | 축소/테마 아이콘 모드에서 인지성 향상 및 렌더 비용 감소 |
| Round 아이콘 리소스 | Manifest `android:roundIcon` 제거 및 관련 리소스 삭제 | Adaptive 마스크 공통 처리, 중복 유지보수 비용 절감 |
| 백업 | `ic_launcher_monochrome_full.xml` 유지 | 원래 복잡한 path 회귀/리디자인 참고용 |

### 후속 개선(옵션)
- Monochrome 실루엣 추가 미세 정렬(픽셀 스냅) 검토
- 다크 모드 전용 adaptive 배경 다이나믹 톤 실험(Material You 컬러 추출)
- 브랜드 디자인 가이드 별도 문서화 (색상 팔레트, 아이콘 관리 표준)

### 테스트 체크리스트
- Android 13 기기: 테마 아이콘 활성화 시 실루엣 왜곡 없음
- Launcher grid: 4x5 / 5x6 레이아웃에서 24~48dp 축소라도 식별 가능
- APK 빌드 후 lint: 아이콘 레이어 누락 경고 없음
