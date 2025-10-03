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
