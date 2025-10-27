# 플레이스토어 "열기" 버튼 리다이렉트 이슈 수정

작성일: 2025-10-27  
중요도: 🔴 높음 (사용자 경험 치명적 이슈)

---

## 문제 상황

### 재현 단계
1. 플레이스토어에서 앱 다운로드 및 설치
2. 앱 실행 → StartActivity (금주 설정 화면) 표시
3. "금주 진행" 버튼 클릭 → RunActivity로 이동, start_time 저장
4. 홈 버튼으로 플레이스토어 복귀
5. **플레이스토어에서 "열기" 버튼 클릭**
6. ❌ **예상**: RunActivity (금주 진행 화면) 복귀
7. ❌ **실제**: StartActivity (금주 설정 화면)가 다시 표시됨

### 증상
- 금주 진행 중인데도 "금주 설정" 화면이 나타남
- 사용자가 진행 상태를 확인할 수 없음
- 혼란스러운 UX (금주가 초기화된 것처럼 보임)

---

## 근본 원인 분석

### 1. AndroidManifest.xml 구조
```xml
<!-- StartActivity: LAUNCHER 인텐트 필터 + singleTask -->
<activity
    android:name=".feature.start.StartActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- RunActivity: singleTask -->
<activity
    android:name=".feature.run.RunActivity"
    android:launchMode="singleTask" />
```

### 2. 플레이스토어 "열기" 동작
- 플레이스토어의 "열기" 버튼은 `ACTION_MAIN + CATEGORY_LAUNCHER` 인텐트를 발생시킴
- 이는 **항상 LAUNCHER로 등록된 Activity를 타겟**으로 함 (= StartActivity)
- 기존 태스크가 있어도 singleTask는 해당 Activity를 태스크 루트로 가져옴

### 3. singleTask 동작 방식
- `singleTask` Activity는 **항상 태스크의 루트**에 위치
- LAUNCHER 인텐트가 발생하면 기존 StartActivity 인스턴스가 포커스됨
- 그 위에 있던 RunActivity는 제거되거나 백스택에 남음

### 4. 왜 문제인가?
```
초기 상태: [StartActivity] → (금주 진행) → [StartActivity → RunActivity]
플레이스토어 "열기": LAUNCHER 인텐트 → [StartActivity] (RunActivity 유실)
```

---

## 해결 방법

### 수정 내용
StartActivity.onCreate()에서 **금주 진행 여부를 체크**하여, 이미 진행 중이면 **즉시 RunActivity로 리다이렉트**하고 StartActivity를 종료.

### 수정된 코드 (StartActivity.kt)
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // ... (스플래시/초기화 코드)
    
    Constants.initializeUserSettings(this)
    Constants.ensureInstallMarkerAndResetIfReinstalled(this)

    // ✅ 금주 진행 중이면 즉시 RunActivity로 리다이렉트 (플레이스토어 "열기" 대응)
    val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    if (startTime > 0) {
        // 금주 진행 중: RunActivity로 이동 후 현재 StartActivity 종료
        val intent = Intent(this, RunActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
        return
    }

    // 이후 정상 StartActivity UI 렌더링 (금주 미진행 시에만 도달)
    // ...
}
```

### 동작 흐름 (수정 후)
```
1. 플레이스토어 "열기" → LAUNCHER 인텐트 → StartActivity.onCreate()
2. StartActivity: start_time 확인 (> 0 이면 진행 중)
3. RunActivity로 Intent 전송 (CLEAR_TOP + SINGLE_TOP)
4. StartActivity.finish() → RunActivity만 화면에 표시
```

---

## 검증 시나리오

### 테스트 케이스 1: 금주 진행 중
1. 앱 실행 → 금주 설정 화면
2. "금주 진행" 버튼 클릭
3. 홈 버튼으로 플레이스토어 복귀
4. "열기" 버튼 클릭
5. ✅ **예상**: RunActivity (금주 진행 화면) 표시
6. ✅ **확인**: start_time, 경과 시간 정상 표시

### 테스트 케이스 2: 금주 미진행
1. 앱 설치 직후 실행
2. 홈 버튼으로 플레이스토어 복귀
3. "열기" 버튼 클릭
4. ✅ **예상**: StartActivity (금주 설정 화면) 표시
5. ✅ **확인**: "금주 진행" 버튼 활성화

### 테스트 케이스 3: 금주 종료 후
1. 금주 진행 → QuitActivity에서 종료 확인
2. start_time = 0으로 초기화됨
3. 홈 버튼으로 플레이스토어 복귀
4. "열기" 버튼 클릭
5. ✅ **예상**: StartActivity (금주 설정 화면) 표시

### 테스트 케이스 4: 드로어 네비게이션 호환성
1. RunActivity에서 드로어 메뉴 열기
2. "금주" 메뉴 선택
3. ✅ **예상**: RunActivity 유지 (이미 진행 중)
4. ✅ **확인**: BaseActivity.handleMenuSelection() 로직과 충돌 없음

---

## 기술적 고려사항

### 왜 onResume이 아닌 onCreate에서 체크?
- `onResume`에서 체크하면 스플래시 화면이 깜빡이는 현상 발생
- `onCreate` 초기에 체크하면 UI 렌더링 전에 즉시 리다이렉트 가능
- return 문으로 조기 종료하여 불필요한 초기화 방지

### FLAG_ACTIVITY_CLEAR_TOP + SINGLE_TOP
- RunActivity도 `singleTask`이므로 기존 인스턴스 재사용
- `CLEAR_TOP`: 백스택에서 RunActivity 위의 모든 Activity 제거
- `SINGLE_TOP`: RunActivity가 이미 top이면 새 인스턴스 생성 안 함

### finish() 타이밍
- `startActivity()` 직후 `finish()` 호출
- StartActivity는 백스택에 남지 않음
- 뒤로가기 시 RunActivity → 홈 화면으로 이동 (StartActivity 거치지 않음)

---

## 대안 검토 (채택하지 않은 이유)

### 대안 1: onNewIntent()에서 처리
```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (start_time > 0) {
        // RunActivity로 이동
    }
}
```
- 문제: `singleTask`는 `onNewIntent`를 호출하지만, 이미 UI가 렌더링된 후
- 화면 깜빡임 발생 (StartActivity → RunActivity 전환이 보임)

### 대안 2: launchMode 변경
```xml
<activity android:launchMode="standard" />
```
- 문제: 드로어 네비게이션에서 StartActivity 중복 인스턴스 생성
- 뒤로가기 시 스택이 복잡해짐

### 대안 3: taskAffinity 분리
```xml
<activity android:taskAffinity=".run" />
```
- 문제: 별도 태스크로 분리되어 최근 앱 목록에 2개 표시
- 플레이스토어 "열기"는 여전히 LAUNCHER 태스크만 포커스

---

## 참고 문서
- Android Activity launchMode: https://developer.android.com/guide/topics/manifest/activity-element#lmode
- Intent flags: https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_CLEAR_TOP
- Play Store app launch behavior: https://issuetracker.google.com/issues/36907463

---

## 요약
- 플레이스토어 "열기"는 항상 LAUNCHER Activity(StartActivity)를 타겟으로 함
- 금주 진행 중에도 StartActivity가 표시되는 문제 발생
- **수정**: StartActivity.onCreate()에서 start_time 체크 → 진행 중이면 RunActivity로 즉시 리다이렉트
- **효과**: 플레이스토어에서 "열기"를 눌러도 항상 올바른 화면(진행 중이면 RunActivity, 아니면 StartActivity) 표시

