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
앱: 앱을 백그라운드로 이동 (StartActivity로 돌아가지 않음)
```
✅ **중요**: 금주 진행 중에는 절대로 StartActivity(금주 설정 화면)으로 돌아가지 않습니다
✅ 뒤로가기 시 앱이 백그라운드로 이동하여 금주가 계속 진행됩니다
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

### 시나리오 4: QuitActivity - 금주 중지/종료 화면

**진입**: RunActivity에서 하단 중지 버튼 클릭으로만 진입 가능 (뒤로가기로 진입 불가)

#### A) 금주 계속 진행 (취소)
```
RunActivity → 중지 버튼 → QuitActivity

사용자 행동:
  1. 뒤로가기 버튼 클릭
  2. 초록 버튼(계속하기) 클릭
  
앱 동작:
  QuitActivity.finish()
  → RunActivity로 복귀 ✅
  → 금주 계속 진행
```

#### B) 금주 종료 (빨간 버튼 롱프레스 1.5초 완료)
```
RunActivity → 중지 버튼 → QuitActivity

사용자 행동:
  빨간 버튼 1.5초 동안 롱프레스 완료 (진행바 100%)
  
앱 동작:
  1. 금주 기록 저장 ✅
  2. SharedPreferences 업데이트:
     - start_time 삭제 (0L로 설정)
     - timer_completed = true
  3. StartActivity로 이동 (FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK) ✅
  4. QuitActivity 종료
  
결과:
  → StartActivity 표시 (금주 설정 화면)
  → 조건: start_time = 0L && timer_completed = true
  → RunActivity로 자동 이동하지 않음 ✅
  → 새로운 금주 시작 가능
```

#### C) 롱프레스 미완료 (1.5초 전에 손 뗌)
```
RunActivity → 중지 버튼 → QuitActivity

사용자 행동:
  빨간 버튼 누름 → 1초만 누르고 손 뗌 (진행바 미완료)
  
앱 동작:
  - 아무 일도 일어나지 않음
  - QuitActivity에 그대로 머물음
  - 다시 선택 가능 (롱프레스 재시도, 취소, 뒤로가기)
```

✅ **중요**: 
- 금주 종료는 **반드시 1.5초 롱프레스 완료**가 필요
- 뒤로가기 또는 초록 버튼 클릭 시 금주 계속 진행 (RunActivity 복귀)
- 금주 종료 후에는 StartActivity로 이동하며, 다시 RunActivity로 자동 이동하지 않음

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

### 3. RunActivity - BackHandler로 백그라운드 이동
```kotlin
// 금주 진행 중에는 뒤로가기로 StartActivity로 돌아가지 않도록 방지
// 뒤로가기 시 앱을 백그라운드로 이동
BackHandler(enabled = true) {
    activity?.moveTaskToBack(true)
}
```
✅ **중요**: 
- 금주 진행 중에는 뒤로가기를 눌러도 StartActivity로 돌아가지 않음
- 앱이 백그라운드로 이동하여 금주가 계속 진행됨
- '중지'는 하단 Stop 버튼으로만 수행

### 4. StartActivity - 금주 종료 상태 감지 및 렌더링 안전장치
```kotlin
// onCreate - DecorView 렌더링 에러 방지
override fun onCreate(savedInstanceState: Bundle?) {
    val splash = if (Build.VERSION.SDK_INT >= 31) installSplashScreen() else null
    
    if (Build.VERSION.SDK_INT >= 31) {
        splash?.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            if (icon != null) {
                icon.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction { provider.remove() }
                    .start()
            } else {
                // iconView가 null인 경우 즉시 제거 (NullPointerException 방지)
                provider.remove()
            }
        }
    }
    
    super.onCreate(savedInstanceState)
    
    // DecorView 렌더링 에러 방지 (BackgroundFallback NullPointerException 회피)
    try {
        window.decorView.setWillNotDraw(false)
    } catch (e: Exception) {
        Log.w("StartActivity", "DecorView setup warning: ${e.message}")
    }
    // ... 나머지 초기화
}

// onResume - 금주 진행 중 재진입 처리
override fun onResume() {
    super.onResume()
    // 재개 시점에서도 배경을 한 번 더 제거하여 잔류 케이스 차단
    window.decorView.post { runCatching { window.setBackgroundDrawable(null) } }
    
    // 금주 진행 중이면 즉시 RunActivity로 이동 (앱 재진입 시 StartActivity가 아닌 RunActivity를 보여주기 위함)
    val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)
    
    if (startTime != 0L && !timerCompleted) {
        // 금주 진행 중: RunActivity로 즉시 전환
        startActivity(Intent(this, RunActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }
}

// StartScreen Composable
@Composable
fun StartScreen(gateNavigation: Boolean = false, onDebugLongPress: (() -> Unit)? = null) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    
    // SharedPreferences 값을 State로 관리하여 변경 감지
    var startTime by remember { mutableLongStateOf(sharedPref.getLong("start_time", 0L)) }
    var timerCompleted by remember { mutableStateOf(sharedPref.getBoolean("timer_completed", false)) }
    
    // SharedPreferences 변경 감지 (Activity 재시작 시 최신 값 로드)
    LaunchedEffect(Unit) {
        startTime = sharedPref.getLong("start_time", 0L)
        timerCompleted = sharedPref.getBoolean("timer_completed", false)
    }
    
    // 진행 중 세션이 있고, 게이트가 내려가 있을 때만 Run 화면으로 이동
    // timer_completed가 true이거나 start_time이 0이면 이동하지 않음
    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, RunActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        return
    }
    // ... 나머지 UI
}

// onCreate - DecorView 렌더링 에러 방지
override fun onCreate(savedInstanceState: Bundle?) {
    val splash = if (Build.VERSION.SDK_INT >= 31) installSplashScreen() else null
    
    if (Build.VERSION.SDK_INT >= 31) {
        splash?.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            if (icon != null) {
                icon.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction { provider.remove() }
                    .start()
            } else {
                // iconView가 null인 경우 즉시 제거 (NullPointerException 방지)
                provider.remove()
            }
        }
    }
    
    super.onCreate(savedInstanceState)
    
    // DecorView 렌더링 에러 방지 (BackgroundFallback NullPointerException 회피)
    try {
        window.decorView.setWillNotDraw(false)
    } catch (e: Exception) {
        Log.w("StartActivity", "DecorView setup warning: ${e.message}")
    }
    // ... 나머지 초기화
}
```
✅ **중요**: 
- State 관리로 금주 종료 후 최신 상태 반영
- iconView null 체크로 스플래시 애니메이션 크래시 방지
- DecorView 렌더링 안전장치로 시스템 레벨 에러 회피
- **onResume에서 금주 진행 중 체크**: 앱 재진입 시 StartActivity가 아닌 RunActivity로 즉시 이동하여 사용자 혼란 방지

### 5. QuitActivity - 금주 종료 시 완전한 초기화
```kotlin
val navigateToStart: () -> Unit = {
    Log.d("QuitActivity", "StartActivity로 이동 시작")
    val i = Intent(activity, StartActivity::class.java).apply {
        // 금주 종료 후에는 새로운 Task로 시작하여 완전히 초기화
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    activity.startActivity(i)
    activity.finish()
}
```
✅ **중요**:
- `FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`로 완전 초기화
- 기존 Activity 스택 모두 제거
- StartActivity가 새로 시작되어 최신 SharedPreferences 로드

### 6. AndroidManifest.xml - singleTask 설정
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
예상: 
  - 앱이 백그라운드로 이동 ✅
  - StartActivity로 돌아가지 않음 ✅
  - 금주가 계속 진행됨 ✅
```

### ✅ 테스트 3: 금주 종료 플로우 (정상 종료)
```
1. RunActivity 실행(금주 진행 중)
2. 하단 중지 버튼 클릭 → QuitActivity 표시
3. 빨간 버튼 1.5초 동안 롱프레스 완료 (진행바 100%)
예상: 
  - 기록 저장 완료 ✅
  - StartActivity로 이동 ✅
  - 뒤로가기 클릭 → 앱 종료/백그라운드 ✅
  - RunActivity로 다시 이동하지 않음 ✅
```

### ✅ 테스트 4: 금주 종료 취소 (계속 진행)
```
1. RunActivity 실행(금주 진행 중)
2. 하단 중지 버튼 클릭 → QuitActivity 표시
3-A. 뒤로가기 클릭
   예상: RunActivity로 복귀 → 금주 계속 ✅
3-B. 초록 버튼(계속하기) 클릭
   예상: RunActivity로 복귀 → 금주 계속 ✅
3-C. 빨간 버튼 0.5초만 누르고 손 뗌 (롱프레스 미완료)
   예상: QuitActivity에 그대로 머물음 → 다시 선택 가능 ✅
```

### ✅ 테스트 5: 금주 종료 후 새 금주 시작
```
1. 금주 종료 완료 (테스트 3 완료 상태)
2. StartActivity에서 목표 일수 설정
3. 시작 버튼 클릭
예상:
  - timer_completed = false로 초기화 ✅
  - start_time = 새로운 시간 ✅
  - RunActivity로 이동 ✅
```

### ✅ 테스트 6: 금주 진행 중 앱 재진입
```
1. RunActivity 실행(금주 진행 중)
2. 홈 버튼 클릭 → 앱 백그라운드로 이동
3. 최근 앱 목록 또는 런처에서 앱 다시 열기
예상:
  - RunActivity로 즉시 복귀 ✅
  - StartActivity가 보이지 않음 ✅
  - 금주가 계속 진행됨 ✅
```

### ✅ 테스트 7: 일반/서브 화면 복귀
- 기존 시나리오와 동일(메인 홈 자동 판단 후 복귀)

---

## 장점(업데이트)
- 종료 시점 팝업/광고 제거로 정책 리스크 해소
- 뒤로가기의 예측 가능성 증대(기본 동작 준수)
- 중지는 명시적 버튼으로만 수행되어 의도치 않은 종료 방지
- 금주 종료 후 StartActivity 완전 초기화로 상태 혼란 방지

---

## ⚠️ 주의사항 및 흔한 혼란

### 1. 롱프레스 미완료 혼란
**증상**: "금주 종료했는데 다시 RunActivity로 이동해요"

**원인**: 빨간 버튼을 1.5초 동안 완전히 누르지 않음
```
잘못된 사용:
  빨간 버튼 클릭 (0.5초) → 손 뗌
  → 아무 일도 안 일어남
  → 뒤로가기 클릭
  → RunActivity로 복귀 ❌

올바른 사용:
  빨간 버튼 1.5초 동안 누름 (진행바 100% 채움)
  → 기록 저장 및 StartActivity 이동 ✅
```

**해결**: 진행바가 완전히 채워질 때까지(100%) 버튼을 누르고 있어야 함

### 2. 버튼 혼동
**증상**: "종료했는데 계속 진행되고 있어요"

**원인**: 빨간 버튼(종료)과 초록 버튼(계속하기) 혼동
```
초록 버튼 (계속하기 ▶️):
  → RunActivity로 복귀 → 금주 계속 진행

빨간 버튼 (종료 ⏹):
  → 1.5초 롱프레스 완료 시 → StartActivity로 이동
```

**해결**: 종료하려면 반드시 빨간 버튼을 1.5초 동안 눌러야 함

### 3. 뒤로가기 동작 혼동
**증상**: "QuitActivity에서 뒤로가기 했는데 종료 안 돼요"

**원인**: QuitActivity는 RunActivity의 서브 화면이므로 뒤로가기 시 부모로 복귀
```
QuitActivity에서 뒤로가기:
  → QuitActivity.finish()
  → RunActivity로 복귀 (금주 계속)
  → 이것은 의도된 동작! ✅

금주 종료 방법:
  → 빨간 버튼 1.5초 롱프레스 완료
  → StartActivity로 이동 ✅
```

**해결**: 금주를 종료하려면 뒤로가기가 아닌 빨간 버튼 롱프레스 필요

### 4. 금주 종료 후 자동 이동 문제 (해결됨)
**이전 문제**: 금주 종료 후 StartActivity에서 다시 RunActivity로 자동 이동

**원인**: 
- Intent 플래그: `FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP` 사용
- 기존 StartActivity 재사용 → 오래된 SharedPreferences 값 사용
- `start_time != 0L` 조건 충족 → RunActivity 자동 이동 ❌

**해결**: 
- Intent 플래그 변경: `FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`
- StartActivity 완전 재시작 → 최신 SharedPreferences 로드
- `start_time = 0L && timer_completed = true` → RunActivity 이동 안 함 ✅

**코드 위치**: `QuitActivity.kt` - `navigateToStart()` 함수

---

## 🔍 문제 해결 가이드

### 금주가 종료되지 않을 때
1. **진행바 확인**: 빨간 버튼을 누를 때 원형 진행바가 100% 채워졌나요?
   - 아니오 → 1.5초 동안 완전히 누르세요
   - 예 → 다음 단계 확인

2. **Logcat 확인**: Android Studio에서 다음 로그 확인
   ```
   D/QuitActivity: 롱프레스 완료 - 금주 종료 처리 시작
   D/QuitActivity: 기록 저장 완료
   D/QuitActivity: 진행 상태 업데이트 완료: timer_completed=true
   D/QuitActivity: StartActivity로 이동 시작
   D/QuitActivity: StartActivity 실행 성공
   ```
   - 로그 없음 → 롱프레스 미완료
   - 로그 있음 → 정상 작동

3. **SharedPreferences 확인**: Device File Explorer에서 확인
   ```
   /data/data/kr.sweetapps.alcoholictimer/shared_prefs/user_settings.xml

   <long name="start_time" value="0" />  ← 0이어야 함
   <boolean name="timer_completed" value="true" />  ← true여야 함
   ```

### 종료 후 다시 RunActivity로 이동할 때
1. **앱 버전 확인**: 최신 버전 사용 중인지 확인
2. **앱 완전 재설치**: 
   ```bash
   adb uninstall kr.sweetapps.alcoholictimer
   gradlew installDebug
   ```
3. **SharedPreferences 초기화**: 설정 > 앱 > AlcoholicTimer > 저장공간 > 데이터 삭제

### 앱 크래시 (NullPointerException: BackgroundFallback.draw)
**증상**: 앱 시작 시 "ViewGroup.getLeft() on a null object reference" 에러

**원인**: Android 시스템 스플래시 애니메이션 중 iconView가 null일 때 발생

**해결 완료**: 
- StartActivity.kt에 null 체크 추가
- DecorView 렌더링 안전장치 추가

**코드 위치**: `StartActivity.kt` - `onCreate()` 스플래시 애니메이션 부분
```kotlin
val icon = provider.iconView
if (icon != null) {
    icon.animate()...
} else {
    provider.remove()  // null일 경우 즉시 제거
}
```

### StartActivity에서 금주 종료 상태 반영 안 됨
**증상**: 금주 종료 후 StartActivity가 RunActivity로 자동 이동함

**원인**: SharedPreferences를 한 번만 읽고 State 업데이트 안 됨

**해결 완료**:
- State 변수로 관리 (`mutableLongStateOf`, `mutableStateOf`)
- `LaunchedEffect(Unit)`로 Activity 재시작 시 최신 값 로드

**코드 위치**: `StartActivity.kt` - `StartScreen` Composable
```kotlin
var startTime by remember { mutableLongStateOf(sharedPref.getLong("start_time", 0L)) }
var timerCompleted by remember { mutableStateOf(sharedPref.getBoolean("timer_completed", false)) }

LaunchedEffect(Unit) {
    startTime = sharedPref.getLong("start_time", 0L)
    timerCompleted = sharedPref.getBoolean("timer_completed", false)
}
```

---

## 변경 이력
- 2025-10-27: **중요 수정** - StartActivity onResume에 금주 진행 중 체크 추가 (앱 재진입 시 RunActivity로 즉시 이동)
- 2025-10-26: **버그 수정** - StartActivity 스플래시 애니메이션 NullPointerException 수정 (iconView null 체크 추가)
- 2025-10-26: **버그 수정** - StartScreen SharedPreferences State 관리 개선 (LaunchedEffect로 최신 값 로드)
- 2025-10-26: **중요 수정** - QuitActivity Intent 플래그 변경 (FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK)
- 2025-10-26: **중요 수정** - RunActivity에 BackHandler 추가하여 뒤로가기 시 StartActivity로 돌아가지 않고 백그라운드로 이동하도록 수정
- 2025-10-26: QuitActivity 시나리오 상세화, 금주 종료 플로우 명확화, 문제 해결 가이드 추가
- 2025-10-26: StartActivity 종료 팝업 제거, RunActivity 뒤로가기 핸들러 제거. 본 문서 최신화
- 2025-01-25: 초기 정의(뒤로가기 팝업/확인 흐름 포함)
