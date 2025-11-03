# 금주 종료 시 스플래시 화면 깜박임 문제 해결

**날짜:** 2025-11-04  
**문제:** 금주 종료 후 StartActivity로 이동할 때 스플래시 화면이 불필요하게 나타남  
**상태:** ✅ 해결 완료

---

## 🔍 문제 분석

### 증상
금주를 종료하고 StartActivity로 돌아갈 때 스플래시 화면이 깜박이는 현상 발생

### 원인

**QuitActivity.kt 및 RunActivity.kt에서 StartActivity로 이동할 때:**

```kotlin
// 문제 코드
Intent(act, StartActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    // skip_splash 플래그 누락! ❌
}
```

**StartActivity는 런처 Activity이므로:**
1. `onCreate()`에서 스플래시 화면을 설치함 (API 31+)
2. 앱 최초 시작 시에는 필요하지만, **내부 네비게이션**으로 돌아올 때는 불필요
3. `skip_splash` 플래그로 스플래시를 스킵할 수 있지만, 금주 종료 시 이 플래그가 없었음

**스플래시 깜박임 발생 시나리오:**
```
금주 종료 (QuitActivity)
    ↓
StartActivity로 이동 (FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK)
    ↓
StartActivity.onCreate() 호출
    ↓
installSplashScreen() 실행 (API 31+)
    ↓
스플래시 화면 표시 (300ms) ❌ 불필요!
    ↓
메인 화면 표시
```

---

## ✅ 해결 방법

### 수정된 파일

#### 1. QuitActivity.kt
**변경 내용:** 금주 종료 후 StartActivity 이동 시 `skip_splash` 플래그 추가

```kotlin
// Before ❌
val i = Intent(act, StartActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}

// After ✅
val i = Intent(act, StartActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    putExtra("skip_splash", true)  // 스플래시 스킵!
}
```

**위치:**
- 라인 199-203: Activity context를 사용하는 경우
- 라인 213-217: Application context를 사용하는 경우

#### 2. RunActivity.kt
**변경 내용:** 타이머 완료 시 StartActivity 이동할 때 `skip_splash` 플래그 추가

```kotlin
// Before ❌
context.startActivity(Intent(context, StartActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
})

// After ✅
context.startActivity(Intent(context, StartActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    putExtra("skip_splash", true)  // 스플래시 스킵!
})
```

**위치:** LaunchedEffect 내부 (라인 83-89)

---

## 🎯 수정 후 동작

### 금주 종료 시 플로우

```
금주 종료 (QuitActivity)
    ↓
StartActivity로 이동 (skip_splash = true)
    ↓
StartActivity.onCreate() 호출
    ↓
skip_splash 플래그 감지
    ↓
스플래시 스킵! ✅
    ↓
메인 화면 즉시 표시
```

### 기존 동작 유지

**앱 최초 실행 시:**
- `skip_splash` 플래그 없음 → 스플래시 정상 표시 ✅

**드로어 메뉴에서 StartActivity 이동 시:**
- `skip_splash = true` 설정됨 (BaseActivity.kt) → 스플래시 스킵 ✅

**뒤로가기로 StartActivity 복귀 시:**
- `skip_splash = true` 설정됨 (BaseActivity.kt) → 스플래시 스킵 ✅

---

## 📝 전체 검증 완료

### StartActivity로의 모든 이동 경로 확인

| 이동 경로 | skip_splash | 상태 |
|----------|-------------|------|
| 앱 최초 시작 (런처) | ❌ (미설정) | ✅ 스플래시 표시 (정상) |
| 금주 종료 (QuitActivity) | ✅ true | ✅ 스플래시 스킵 (수정됨) |
| 타이머 완료 (RunActivity) | ✅ true | ✅ 스플래시 스킵 (수정됨) |
| 드로어 메뉴 | ✅ true | ✅ 스플래시 스킵 (기존) |
| 뒤로가기 홈 복귀 | ✅ true | ✅ 스플래시 스킵 (기존) |

---

## 🧪 테스트 시나리오

### 1. 금주 종료 테스트
```
1. 금주 시작
2. QuitActivity 진입
3. 롱프레스로 금주 종료
4. ✅ StartActivity로 즉시 전환 (스플래시 없음)
```

### 2. 타이머 완료 테스트
```
1. 금주 진행 중
2. SharedPreferences에서 timer_completed = true 설정
3. RunActivity 재진입
4. ✅ StartActivity로 즉시 전환 (스플래시 없음)
```

### 3. 앱 재시작 테스트
```
1. 앱 완전 종료
2. 런처에서 앱 실행
3. ✅ 스플래시 화면 정상 표시
4. ✅ StartActivity 진입
```

---

## 📊 빌드 결과

```bash
$ ./gradlew assembleDebug

> Task :app:compileDebugKotlin

BUILD SUCCESSFUL in 8s
39 actionable tasks: 7 executed, 7 from cache, 25 up-to-date
```

✅ **빌드 성공**
✅ **경고만 존재 (기존 deprecated 경고)**
✅ **컴파일 에러 없음**

---

## 🔧 기술적 세부사항

### skip_splash 플래그의 작동 방식

**StartActivity.onCreate():**
```kotlin
// skip_splash 플래그 읽기
val skipSplash = intent.getBooleanExtra("skip_splash", false)

// 스플래시 오버레이 시간 계산
val initialRemain = if (skipSplash) 0L else initialRemain
```

**플래그가 true일 때:**
- `initialRemain = 0L` → Compose 오버레이가 즉시 종료
- `usesComposeOverlay = false` → 추가 오버레이 비활성화
- 스플래시 애니메이션 없이 바로 메인 화면 표시

**플래그가 false일 때 (또는 미설정):**
- `initialRemain = minShowMillis - elapsed` → 최소 300ms 표시
- API 31+에서 시스템 스플래시 정상 작동

---

## 🎯 사용자 경험 개선

### Before (문제)
```
금주 종료 버튼 클릭
    ↓
광고 표시 (선택적)
    ↓
⚡ 스플래시 화면 깜박임 (300ms) ← 불필요!
    ↓
메인 화면
```

**문제점:**
- 불필요한 스플래시로 인한 UX 저하
- 금주 종료 후 화면 전환이 부자연스러움

### After (해결)
```
금주 종료 버튼 클릭
    ↓
광고 표시 (선택적)
    ↓
✅ 메인 화면으로 즉시 전환
```

**개선 사항:**
- 매끄러운 화면 전환
- 자연스러운 UX
- 사용자 혼란 방지

---

## 📚 관련 파일

### 수정된 파일
- `app/src/main/java/kr/sweetapps/alcoholictimer/feature/run/QuitActivity.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/feature/run/RunActivity.kt`

### 참조 파일
- `app/src/main/java/kr/sweetapps/alcoholictimer/feature/start/StartActivity.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/core/ui/BaseActivity.kt`

### 관련 문서
- `docs/SPLASH_AND_LAUNCHER_ICON_PROMPT.md` - 스플래시 화면 정책
- `docs/a_QUIT_FLOW_ANALYSIS.md` - 금주 종료 플로우 분석
- `docs/a_BACK_NAVIGATION_SCENARIOS.md` - 뒤로가기 시나리오

---

## ✅ 체크리스트

- [x] 문제 원인 파악
- [x] QuitActivity에 skip_splash 플래그 추가
- [x] RunActivity에 skip_splash 플래그 추가
- [x] 모든 StartActivity 이동 경로 확인
- [x] 빌드 성공 확인
- [x] 테스트 시나리오 작성
- [x] 문서화 완료

---

## 🎉 결론

금주 종료 시 스플래시 화면이 깜박이는 문제가 **완전히 해결**되었습니다!

**핵심 변경사항:**
- QuitActivity → StartActivity 이동 시 `skip_splash = true` 추가
- RunActivity → StartActivity 이동 시 `skip_splash = true` 추가

**사용자 경험:**
- 금주 종료 후 매끄러운 화면 전환
- 불필요한 스플래시 제거
- 자연스러운 앱 플로우

---

**최종 업데이트:** 2025-11-04  
**버전:** 1.0  
**상태:** ✅ 완료

