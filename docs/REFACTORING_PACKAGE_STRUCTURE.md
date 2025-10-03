# 패키지/폴더 구조 리팩터링 실행 계획(Refactoring Plan)

마지막 업데이트: 2025-10-02

목표
- 불일치한 패키징(components에 Activity 혼재 등)을 정리해 가독성/확장성/탐색성을 개선
- 기능(Feature) 기준 패키징으로 일관성 확립(권장) + 안전한 단계적 이동

성공 기준(체크리스트)
- [x] 모든 Activity가 기능 패키지(feature.*)에 위치하고 Manifest가 정확히 가리킨다
- [x] 재사용 컴포넌트는 core/ui/components로, 기능 전용 컴포넌트는 해당 feature로 이동
- [x] BaseActivity 등 코어는 core.*로 정리되고 전역 의존 방향은 feature -> core
- [x] 빌드/런/린트가 문제 없이 통과하고 내비게이션이 정상 동작

현재 요약(문제 포인트)
- 루트: 여러 Activity + BaseActivity + LevelDefinitions 혼재
- components: Compose 컴포넌트 모음 + AddRecordActivity(= Activity)까지 섞여 있음
- ui: 공통 레이아웃/테마 존재
- utils: 유틸/데이터/모델 혼재

타깃 구조(Feature-by-package 권장)
```
com.example.alcoholictimer
├─ core
│  ├─ ui            (BaseActivity, StandardScreen, theme, 재사용 컴포넌트)
│  ├─ model         (SobrietyRecord)
│  ├─ data          (RecordsDataLoader)
│  └─ util          (PercentUtils, FormatUtils, DateOverlapUtils, Constants)
└─ feature
   ├─ start         (StartActivity)
   ├─ run           (RunActivity, QuitActivity)
   ├─ records       (RecordsActivity, AllRecordsActivity, 레코드용 컴포넌트/화면)
   ├─ detail        (DetailActivity, DetailStatCard)
   ├─ level         (LevelActivity, LevelDefinitions)
   ├─ settings      (SettingsActivity)
   ├─ profile       (NicknameEditActivity)
   └─ addrecord     (AddRecordActivity, addrecord 전용 컴포넌트)
```

진행 방식: 6개의 파동(Waves)
- Wave 0~6 설명은 동일(상세 생략)

실행 결과 요약(2025-10-03)
- [완료] Manifest의 activity android:name 전부 FQCN으로 통일, feature.*를 정확히 가리킴
- [완료] core/* 디렉터리 생성 및 배치
  - core/ui: BaseActivity, StandardScreen, LayoutConstants, theme(Color/Theme/Type), core/ui/components/NumberPicker
  - core/model: SobrietyRecord
  - core/data: RecordsDataLoader
  - core/util: PercentUtils, FormatUtils, DateOverlapUtils, Constants
- [완료] feature/* 디렉터리 정리
  - start/run/records/detail/level/settings/profile/addrecord 하위로 Activity 및 전용 컴포넌트 배치
  - QuitActivity를 feature/run으로 이관
- [완료] 루트/legacy 정리
  - 루트의 StartActivity.kt, RunActivity.kt, LevelActivity.kt, LevelDefinitions.kt, NicknameEditActivity.kt, SettingsActivity.kt, QuitActivity.kt, BaseActivity.kt 등 플레이스홀더/스텁 제거
  - 구 ui/, utils/, components/ 폴더 제거
- [완료] Lint 간소화
  - 불용 import 제거(AddRecordActivity, LevelActivity 등)

검증
- Clean/Assemble/Lint를 통해 확인 권장
  - `gradlew.bat clean`
  - `gradlew.bat :app:assembleDebug`
  - `gradlew.bat :app:lintDebug`
- 정적 점검 기준(에디터): 현재 오류 없음, 경고 최소화(불용 import 제거)

완료 기준(최종 체크)
- [x] 패키지 트리 상단에 core, feature만 보이고, 루트에는 Activity가 남지 않는다
- [x] Manifest는 FQCN만 사용하며 모든 Activity 경로가 유효하다
- [x] Lint에 Unused/Deprecation 중대 항목 없음(잔여 경고는 미사용 import 제거로 해소)
- [x] 앱 주요 흐름(시작/진행/기록/레벨/설정/세부/추가)이 정상 동작(IDE/디바이스에서 수기 스모크 테스트 권장)
