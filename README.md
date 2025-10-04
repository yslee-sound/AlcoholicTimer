# AlcoholicTimer

AlcoholicTimer는 금주(절주) 습관 형성을 돕는 Android 앱입니다. 핵심 기능: 금주 타이머, 목표 진행률/레벨, 기록 관리(목록/상세/전체보기), 기간별 통계(주/월/년/전체), 주간 성공률 시각화. UI는 Jetpack Compose(Material3) 기반이며 데이터는 로컬(SharedPreferences/JSON) 저장을 사용합니다.

## 주요 기능
- 금주 기록 생성 및 실시간 진행률 표시 (목표 일수 대비 % 및 레벨)
- 주 / 월 / 년 / 전체 통계 (평균·최대 지속일, 누적 금주일)
- 주간 성공률: 해당 주 7일 중 금주 유지일 비율 (중복 기록 병합 후 산출, 7일 모두 유지 시 100%)
- Adaptive & Monochrome 런처 아이콘 (Android 13 테마 아이콘 대응)

## 빠른 시작 (Windows / Gradle Wrapper)
```bat
REM 클린 & 디버그 빌드
gradlew.bat clean
gradlew.bat :app:assembleDebug

REM Lint 검사
gradlew.bat :app:lintDebug
```
생성 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 문서
- 기획 사양: [docs/APP_SPEC.md](./docs/APP_SPEC.md)
- UX 흐름: [docs/UX_FLOW.md](./docs/UX_FLOW.md)
- 패키지 구조 리팩터링 계획: [docs/REFACTORING_PACKAGE_STRUCTURE.md](./docs/REFACTORING_PACKAGE_STRUCTURE.md)
- 아이콘 디자인 가이드(상세): [docs/ICON_DESIGN.md](./docs/ICON_DESIGN.md)
- 출시 준비 & 배포 체크리스트: [docs/APP_RELEASE_PLAN.md](./docs/APP_RELEASE_PLAN.md)

## 디렉터리 구조 개요
- `app/` : 애플리케이션 본체(Compose UI, Activity, 유틸)
- `docs/` : 사양/UX/배포/디자인 문서

## 주간 성공률 산식 (요약)
```
주간 성공률 = (해당 주 내 금주 유지 총 일수(중복 병합) / 7) * 100  (반올림, 최대 100)
```
- 여러 기록이 겹칠 경우 겹치는 구간은 한 번만 포함
- 월/년/전체 화면의 "성공률"은 기존 시도별 목표진행률 평균 로직 유지

## 아이콘 (요약)
| 항목 | 값 |
|------|----|
| Inset | 18dp (Adaptive 캔버스 108dp 기준) |
| Foreground Scale | 1.43 (과밀 완화) |
| Monochrome | 단일 path, 테마 아이콘 대비 확보 |
| Colors | FG #C6283A / BG #FFFFFF |
| Round Icon | 별도 리소스 제거 (Adaptive 단일 유지) |

자세한 변경 이력·QA 체크리스트는 [ICON_DESIGN.md](./docs/ICON_DESIGN.md) 참고.

## 개발 / 품질 참고
| 작업 | 명령 |
|------|------|
| Debug 빌드 | `gradlew.bat :app:assembleDebug` |
| Release 번들 | `gradlew.bat clean :app:bundleRelease` |
| Lint (중요) | `gradlew.bat lintVitalRelease` |
| 유닛 테스트(있다면) | `gradlew.bat testDebug` |

## 향후 확장 아이디어 (요약)
- 알림/리마인더, 위젯, 클라우드 동기화/백업
- Crash/Analytics 도입, In-App Review, 다국어 지원

## 라이선스
(추후 명시 예정)

## 기여
내부 프로젝트 기준이나 개선 아이디어(자동화, 분석, 접근성) 제안 환영. 문서/코드 변경 시 일관성 체크 후 PR 권장.

---
이 README는 모듈/아이콘 상세를 통합·요약한 최신 버전입니다. 세부 디자인/배포/작업 흐름은 docs 디렉터리를 참조하세요.
