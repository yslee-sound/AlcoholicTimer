# AlcoholicTimer

AlcoholicTimer는 금주(절주) 습관 형성을 돕는 Android 앱입니다. 핵심 기능: 금주 타이머, 목표 진행률/레벨, 기록 관리(목록/상세/전체보기), 기간별 통계(주/월/년/전체), 주간 성공률 시각화. UI는 Jetpack Compose(Material3) 기반이며 데이터는 로컬(SharedPreferences/JSON) 저장을 사용합니다.

## 주요 기능
- 금주 기록 생성 및 실시간 진행률 표시 (목표 일수 대비 % 및 레벨)
- 주 / 월 / 년 / 전체 통계 (평균·최대 지속일, 누적 금주일)
- 주간 성공률: 해당 주 7일 중 금주 유지일 비율 (중복 기록 병합 후 산출, 7일 모두 유지 시 100%)
- Adaptive & Monochrome 런처 아이콘 (Android 13 테마 아이콘 대응)

## 버전 / 빌드 전략 (요약)
- Semantic Versioning: `MAJOR.MINOR.PATCH` → `versionName`
- `versionCode`: CI 환경에서 환경변수 `VERSION_CODE` (없으면 임시 YYYYMMDD 대체)
- Release 빌드: R8 코드 난독화 + 리소스 축소 활성화
- Keystore 서명: 환경변수 기반 (미설정 시 unsigned 번들 생성)
  - `KEYSTORE_PATH`, `KEYSTORE_STORE_PW`, `KEY_ALIAS`, `KEY_PASSWORD`

## 빠른 시작 (Windows / Gradle Wrapper)
```bat
REM 클린 & 디버그 빌드
gradlew.bat clean
gradlew.bat :app:assembleDebug

REM Lint 검사 (Debug / Release Vital)
gradlew.bat :app:lintDebug
gradlew.bat :app:lintVitalRelease

REM 유닛 테스트 (JVM)
gradlew.bat :app:testDebugUnitTest

REM 릴리스 번들 (환경변수 설정 후)
SET VERSION_CODE=20251005
SET VERSION_NAME=1.0.0
REM (선택) 서명 키 설정
REM SET KEYSTORE_PATH=keystore.jks
REM SET KEYSTORE_STORE_PW=****
REM SET KEY_ALIAS=****
REM SET KEY_PASSWORD=****
gradlew.bat clean :app:bundleRelease
```
생성 산출물:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

## 문서
- 기획 사양: [docs/APP_SPEC.md](./docs/APP_SPEC.md)
- UX 흐름: [docs/UX_FLOW.md](./docs/UX_FLOW.md)
- 패키지 구조 리팩터링 계획: [docs/REFACTORING_PACKAGE_STRUCTURE.md](./docs/REFACTORING_PACKAGE_STRUCTURE.md)
- 아이콘 디자인 가이드: [docs/ICON_DESIGN.md](./docs/ICON_DESIGN.md)
- 출시 준비 & 배포 체크리스트: [docs/APP_RELEASE_PLAN.md](./docs/APP_RELEASE_PLAN.md)
- 변경 이력: [CHANGELOG.md](./CHANGELOG.md)

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
| Lint (Debug) | `gradlew.bat :app:lintDebug` |
| Lint Vital(Release) | `gradlew.bat :app:lintVitalRelease` |
| 유닛 테스트 | `gradlew.bat :app:testDebugUnitTest` |

### 테스트 커버리지 확장 (현재 포함)
- DateOverlapUtils (기간 겹침, 경계, 비겹침)
- FormatUtils (일+시간 포맷, 경계 반올림, 잘못된 입력)
- PercentUtils (비율/반올림, 음수/경계)
- SobrietyRecord (퍼센트 계산, 레벨/타이틀, JSON 직렬화)

추가 제안:
- 통계(주/월/연속일) 계산 모듈 추출 & 순수 함수화 후 테스트
- 목표 달성 상태(isCompleted) 파생 로직 분리 테스트

## CI (예시 워크플로 개요)
GitHub Actions 예:
```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Grant execute
        run: chmod +x gradlew
      - name: Build & Test
        run: ./gradlew clean :app:testDebugUnitTest :app:lintDebug --no-daemon
      - name: Assemble Release (dry)
        run: ./gradlew :app:bundleRelease -PVERSION_CODE=$(date +%Y%m%d) -PVERSION_NAME=1.0.0-dry --no-daemon || true
```

## CHANGELOG 정책
`CHANGELOG.md`는 Keep a Changelog 형식 유지. 기능/수정 PR 시 Unreleased 섹션에 항목 추가 후 릴리스 태그 시 섹션 분리.

## 환경변수 주입 방법(Windows Powershell 예)
```powershell
$env:VERSION_CODE=(Get-Date -UFormat %Y%m%d)
$env:VERSION_NAME="1.1.0"
$env:KEYSTORE_PATH="keystore.jks"
$env:KEYSTORE_STORE_PW="****"
$env:KEY_ALIAS="****"
$env:KEY_PASSWORD="****"
./gradlew.bat :app:bundleRelease
```

## 향후 확장 아이디어 (요약)
- 알림/리마인더, 위젯, 클라우드 동기화/백업
- Crash/Analytics 도입, In-App Review, 다국어 지원
- Jacoco 커버리지 리포트 & Detekt 정적 분석

## 라이선스
(추후 명시 예정)

## 기여
내부 프로젝트 기준이나 개선 아이디어(자동화, 분석, 접근성) 제안 환영. 문서/코드 변경 시 일관성 체크 후 PR 권장.

---
이 README는 모듈/아이콘 및 릴리스 품질 전략을 포함한 최신 버전입니다. 세부 디자인/배포/작업 흐름은 docs 디렉터리를 참조하세요.
