# 뒤로가기 동작 시나리오 정의

## 현재 상태 분석 (2025-10-26 - 최신 적용 반영)

### 구현된 방식
- **구조**: Multi-Activity 구조 (각 화면이 독립된 Activity)
- **메인 홈 개념**: StartActivity(금주 설정) / RunActivity(금주 진행)
- **네비게이션**: 드로어 메뉴를 통한 화면 전환
- **Back Stack**: 메인 홈만 singleTask, 나머지는 standard
- **핵심 원칙**: “뒤로가기 = 자연스러운 이동/종료”. 종료 유도 팝업/광고 없음

---

## 메인 홈 화면 개념

### 🏠 메인 홈 (singleTask)
1. **StartActivity** - 금주 설정 화면 (금주 진행 전)
2. **RunActivity** - 금주 진행 화면 (금주 진행 중)

**메인 홈 결정 로직:**
```kotlin
금주 진행 여부 확인 (start_time > 0)
  → 진행 중: RunActivity가 메인 홈
  → 진행 전: StartActivity가 메인 홈
```

### 📱 일반 화면 (standard)
- **RecordsActivity** - 금주 기록
- **LevelActivity** - 레벨
- **SettingsActivity** - 설정
- **AboutActivity** - 앱 정보

### 📄 서브 화면 (2단계, standard)
- **AllRecordsActivity** ← RecordsActivity
- **DetailActivity** ← RecordsActivity
- **AboutLicensesActivity** ← AboutActivity
- **NicknameEditActivity** ← 드로어
- **AddRecordActivity** ← RecordsActivity
- **QuitActivity** ← RunActivity(중지 버튼으로만 진입)

---

## 적용된 뒤로가기 동작

### 시나리오 1: 메인 홈에서 뒤로가기
**StartActivity (금주 설정 화면)**
```
사용자: 뒤로가기 버튼 클릭
앱: 팝업 없이 시스템 기본 동작(앱 종료/백그라운드)
```
✅ 종료 팝업 및 광고 없음 — 간결하고 안전한 종료

**RunActivity (금주 진행 화면)**
```
사용자: 뒤로가기 버튼 클릭
앱: 시스템 기본 동작(이전 화면이 있으면 복귀, 루트면 종료/백그라운드)
```
✅ '금주 종료' 확인 화면은 뒤로가기로 열리지 않음(중지 버튼으로만 진입)

---

### 시나리오 2: 일반 화면에서 뒤로가기 → 메인 홈으로
**RecordsActivity / LevelActivity / SettingsActivity / AboutActivity**

**예시 1: 금주 진행 전**
```
Start(메인 홈) → 드로어 → Records → 드로어 → Level
사용자: 뒤로가기
앱: Level 종료 → Start(메인 홈)로 복귀
```

**예시 2: 금주 진행 중**
```
Run(메인 홈) → 드로어 → Records → 드로어 → Settings
사용자: 뒤로가기
앱: Settings 종료 → Run(메인 홈)로 복귀
```

✅ 구현 완료
- BackHandler로 뒤로가기 시 `navigateToMainHome()` 호출
- 금주 진행 여부에 따라 자동으로 Start 또는 Run으로 이동

---

### 시나리오 3: 2단계 서브 화면 → 부모 → 메인 홈

**AboutActivity → AboutLicensesActivity**
```
메인 홈 → 드로어 → About → "오픈 라이선스" 클릭 → Licenses

사용자: Licenses에서 뒤로가기
앱: Licenses 종료 → About으로 복귀

사용자: About에서 뒤로가기
앱: About 종료 → 메인 홈으로 복귀
```

**RecordsActivity → AllRecordsActivity**
```
메인 홈 → 드로어 → Records → "전체보기" 클릭 → AllRecords

사용자: AllRecords에서 뒤로가기
앱: AllRecords 종료 → Records로 복귀

사용자: Records에서 뒤로가기
앱: Records 종료 → 메인 홈으로 복귀
```

✅ 정상 작동
- 서브 화면은 `showBackButton = true`로 finish()
- 부모 화면은 BackHandler로 메인 홈 복귀

---

### 시나리오 4: QuitActivity → RunActivity 복귀
```
RunActivity에서 중지 버튼 클릭 → QuitActivity

사용자: 뒤로가기(또는 취소)
앱: QuitActivity 종료 → RunActivity로 복귀
```

✅ 의도된 흐름 유지(중지는 버튼으로만 시작)

---

## 구현 세부사항(발췌)

### 1. BaseActivity.kt - 공통 함수
```kotlin
protected fun navigateToMainHome() { /* 기존 구현 유지 */ }
```

### 2. 각 일반 화면 - BackHandler
```kotlin
BackHandler(enabled = true) { navigateToMainHome() }
```

### 3. RunActivity - 뒤로가기 핸들러 제거
- 이전: 뒤로가기 → QuitActivity 진입(실수 방지)
- 현재: 뒤로가기는 시스템 기본 동작. ‘중지’는 하단 Stop 버튼으로만 수행

### 4. AndroidManifest.xml - singleTask 설정
- StartActivity / RunActivity: singleTask  
- 그 외: standard  
(기존과 동일)

---

## 테스트 시나리오(업데이트)

### ✅ 테스트 1: StartActivity 뒤로가기
```
1. StartActivity 실행
2. 뒤로가기 클릭
예상: 팝업 없이 앱 종료/백그라운드
```

### ✅ 테스트 2: RunActivity 뒤로가기
```
1. RunActivity 실행(금주 진행 중)
2. 뒤로가기 클릭
예상: Quit 화면 없이 기본 동작(이전 화면 복귀 또는 종료)
```

### ✅ 테스트 3: 중지 플로우
```
1. RunActivity 실행
2. 하단 중지 버튼 클릭
예상: QuitActivity 표시 → 취소 시 Run으로 복귀, 완료 시 기록 저장/후속 이동
```

### ✅ 테스트 4: 일반/서브 화면 복귀
- 기존 시나리오와 동일(메인 홈 자동 판단 후 복귀)

---

## 장점(업데이트)
- 종료 시점 팝업/광고 제거로 정책 리스크 해소
- 뒤로가기의 예측 가능성 증대(기본 동작 준수)
- 중지는 명시적 버튼으로만 수행되어 의도치 않은 종료 방지

---

## 변경 이력
- 2025-10-26: StartActivity 종료 팝업 제거, RunActivity 뒤로가기 핸들러 제거. 본 문서 최신화
- 2025-01-25: 초기 정의(뒤로가기 팝업/확인 흐름 포함)
