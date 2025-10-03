# 패키지/폴더 구조 리팩터링 실행 계획(Refactoring Plan)

마지막 업데이트: 2025-10-02

목표
- 불일치한 패키징(components에 Activity 혼재 등)을 정리해 가독성/확장성/탐색성을 개선
- 기능(Feature) 기준 패키징으로 일관성 확립(권장) + 안전한 단계적 이동

성공 기준(체크리스트)
- [ ] 모든 Activity가 기능 패키지(feature.*)에 위치하고 Manifest가 정확히 가리킨다
- [ ] 재사용 컴포넌트는 core/ui/components로, 기능 전용 컴포넌트는 해당 feature로 이동
- [ ] BaseActivity 등 코어는 core.*로 정리되고 전역 의존 방향은 feature -> core
- [ ] 빌드/런/린트가 문제 없이 통과하고 내비게이션이 정상 동작

현재 요약(문제 포인트)
- 루트: 여러 Activity + BaseActivity + LevelDefinitions 혼재
- components: Compose 컴포넌트 모음 + AddRecordActivity(= Activity)까지 섞여 있음
- ui: 공통 레이아웃/테마 존재
- utils: 유틸/데이터/모델 혼재

타겟 구조(Feature-by-package 권장)
```
com.example.alcoholictimer
├─ core
│  ├─ ui            (BaseActivity, StandardScreen, theme, 재사용 컴포넌트)
│  ├─ model         (SobrietyRecord)
│  ├─ data          (RecordsDataLoader)
│  └─ util          (PercentUtils, FormatUtils, DateOverlapUtils, Constants)
└─ feature
   ├─ start         (StartActivity)
   ├─ run           (RunActivity)
   ├─ records       (RecordsActivity, AllRecordsActivity, 레코드용 컴포넌트/화면)
   ├─ detail        (DetailActivity, DetailStatCard)
   ├─ level         (LevelActivity, LevelDefinitions)
   ├─ settings      (SettingsActivity)
   ├─ profile       (NicknameEditActivity)
   └─ addrecord     (AddRecordActivity, addrecord 전용 컴포넌트)
```

세부 매핑(초안)
- core/ui
  - `BaseActivity.kt`, `ui/StandardScreen.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt`, `ui/theme/Color.kt`
  - 재사용 컴포넌트: `components/NumberPicker.kt` (여러 기능에서 공용 사용)
- core/model
  - `utils/SobrietyRecord.kt`
- core/data
  - `utils/RecordsDataLoader.kt`
- core/util
  - `utils/PercentUtils.kt`, `utils/FormatUtils.kt`, `utils/DateOverlapUtils.kt`, `utils/Constants.kt`
- feature/records
  - `RecordsActivity.kt`, `AllRecordsActivity.kt`
  - `components/AllRecords.kt`, `components/RecordsScreen.kt`,
    `components/RecordSummaryCard.kt`, `components/StatisticsCardsSection.kt`,
    `components/PeriodSelectionSection.kt`, `components/MonthPickerBottomSheet.kt`,
    `components/WeekPickerBottomSheet.kt`, `components/YearPickerBottomSheet.kt`
- feature/addrecord
  - `components/AddRecordActivity.kt`, `components/TripleDigitNumberPicker.kt`
- feature/detail
  - `DetailActivity.kt`, `components/DetailStatCard.kt`
- feature/level
  - `LevelActivity.kt`, `LevelDefinitions.kt`
- feature/settings
  - `SettingsActivity.kt`
- feature/profile
  - `NicknameEditActivity.kt`
- feature/run
  - `RunActivity.kt`
- feature/start
  - `StartActivity.kt`

진행 방식: 6개의 파동(Waves)
- Wave 0: 준비
  - 새 브랜치 생성(refactor/packages), 커밋 단위(기능 단위) 유지
  - Android Studio에서 ‘Project’ 뷰로 전환 → 리팩터링/이동 시 패키지 선언 자동 변경 사용
- Wave 1: addrecord 기능 정리(작게 시작)
  1) `components/AddRecordActivity.kt` → `feature/addrecord/AddRecordActivity.kt`
  2) `components/TripleDigitNumberPicker.kt` → `feature/addrecord/components/TripleDigitNumberPicker.kt`
  3) Manifest의 activity android:name을 새 FQCN으로 변경
  4) 빌드/런/QA(기록 추가 플로우)
- Wave 2: records 기능 묶기
  1) `RecordsActivity.kt`, `AllRecordsActivity.kt` 이동
  2) 기록 관련 Compose 파일들을 `feature/records/...` 아래로 이동
  3) imports 및 패키지 경로 정리(BaseActivity Navigate 참조 포함)
  4) 빌드/런/QA(Records/AllRecords 화면)
- Wave 3: detail & level
  1) `DetailActivity.kt`, `components/DetailStatCard.kt` → feature/detail
  2) `LevelActivity.kt`, `LevelDefinitions.kt` → feature/level
  3) 빌드/런/QA(Detail/Level 화면)
- Wave 4: start & run & settings & profile
  1) 각 Activity를 해당 feature로 이동
  2) BaseActivity의 메뉴 네비게이션 참조 업데이트
  3) 빌드/런/QA(Drawer → 각 화면 이동)
- Wave 5: core 정리
  1) `ui/**`, `utils/**`를 core 하위로 배치(위 매핑 기준)
  2) 재사용 컴포넌트 NumberPicker는 core/ui/components로 이동
  3) 빌드/런/QA(앱 전반)
- Wave 6: 마무리/정리
  - Manifest 상대 경로를 FQCN으로 통일(이동에 강함)
  - 불필요 import/Dead code 정리 → Lint/Detekt
  - 문서(본 문서/ARCHITECTURE.md) 갱신

Manifest 업데이트 가이드
- 위치: `app/src/main/AndroidManifest.xml`
- 예시(변경 전)
  - `<activity android:name=".components.AddRecordActivity" ... />`
- 예시(변경 후)
  - `<activity android:name="com.example.alcoholictimer.feature.addrecord.AddRecordActivity" ... />`
- 권장: 모든 activity android:name을 FQCN으로 표기

검증 절차(각 Wave 마다 반복)
1) 클린/빌드/린트
   - `gradlew.bat clean`
   - `gradlew.bat :app:assembleDebug`
   - `gradlew.bat :app:lintDebug`
2) 간단 스모크 테스트
   - 앱 실행 → 메뉴 이동/기능 흐름 확인
3) 실패 시 롤백
   - 직전 커밋으로 되돌리거나 해당 Wave 범위에서만 수정 재시도

롤백/위험 관리
- 작은 단위(Wave)로 커밋, 실패 범위 제한
- Manifest는 즉시 FQCN으로 통일하여 경로 변경에 강하게 유지
- Preview 전용 코드(@Preview) import 깨짐 주의 → IDE 자동 수정

네이밍/규칙(요약)
- 화면 루트 Composable: XxxScreen, 카드: XxxCard, 선택기: XxxPicker, 시트: XxxBottomSheet
- 재사용(범용) 컴포넌트는 core/ui/components, 기능 종속은 feature/*/components
- 의존 방향: feature → core(역의존 금지)

완료 기준(최종 체크)
- [ ] 패키지 트리 상단에 core, feature만 보이고,루트에는 Activity가 남지 않는다
- [ ] Manifest는 FQCN만 사용하며 모든 Activity 경로가 유효하다
- [ ] Lint에 Unused/Deprecation 중대 항목 없음
- [ ] 앱 주요 흐름(시작/진행/기록/레벨/설정/세부/추가)이 정상 동작

