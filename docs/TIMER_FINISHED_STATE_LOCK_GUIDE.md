# 타이머 만료 후 상태 잠금(State Lock) 및 전면 광고 연동 최종 가이드

## 📌 개요

타이머 만료 시 **1번째 탭을 완전히 점유**하고, **'새 타이머 시작' 버튼만이 유일한 해제 경로**가 되도록 구현된 최종 UX 흐름입니다.

---

## 🔒 핵심 개념: 상태 잠금(State Lock)

### 상태 잠금이란?

`TimerStateRepository.isTimerFinished() == true` 상태에서는:
- ✅ 1번째 탭이 **무조건 만료 UI(FinishedScreen)**만 표시
- ✅ 사용자가 앱을 재시작하거나 다른 탭에서 돌아와도 **만료 UI 유지**
- ✅ **'새 타이머 시작' 버튼**만이 이 상태를 해제할 수 있음

### 왜 필요한가?

- 사용자가 타이머 완료를 인지하지 못하고 넘어가는 것 방지
- 완료 기록을 확실히 확인하도록 유도
- 일관된 UX 제공

---

## 🎯 최종 UX 흐름

```
타이머 만료 (TimerStateRepository.setTimerFinished(true))
  ↓
[상태 잠금 시작]
  ↓
1번째 탭 진입 시 무조건 FinishedScreen 표시
  ├─ 앱 재시작 → FinishedScreen
  ├─ 다른 탭에서 복귀 → FinishedScreen
  └─ 백그라운드 복귀 → FinishedScreen
  ↓
사용자 선택:
  ├─ [결과 확인] 버튼
  │   ├─ AdPolicyManager.shouldShowInterstitialAd() 체크
  │   ├─ 쿨타임 통과 → 전면 광고 표시
  │   ├─ 광고 닫기/실패 → Detail 화면 이동
  │   └─ [중요] 만료 상태 유지 (isFinished = true)
  │   ↓
  │   Detail 화면에서 뒤로가기 → 다시 FinishedScreen
  │
  └─ [새 타이머 시작] 버튼
      ├─ TimerStateRepository.setTimerFinished(false) ← [유일한 해제 경로]
      ├─ TimerStateRepository.setTimerActive(false)
      └─ Start 화면으로 이동
      ↓
      [상태 잠금 해제]
```

---

## 📋 구현 상세

### 1. 상태 관리 (TimerStateRepository)

**위치**: `data/repository/TimerStateRepository.kt`

```kotlin
object TimerStateRepository {
    // 만료 상태 저장
    fun setTimerFinished(isFinished: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(Constants.PREF_TIMER_COMPLETED, isFinished)
            apply()
        }
        Log.d(TAG, "타이머 만료 상태 저장: $isFinished")
    }

    // 만료 상태 확인
    fun isTimerFinished(): Boolean {
        return sharedPreferences?.getBoolean(Constants.PREF_TIMER_COMPLETED, false) ?: false
    }

    // 타이머 작동 상태 저장
    fun setTimerActive(isActive: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_TIMER_ACTIVE, isActive)
            apply()
        }
    }

    // 타이머 작동 상태 확인
    fun isTimerActive(): Boolean {
        return sharedPreferences?.getBoolean(KEY_IS_TIMER_ACTIVE, false) ?: false
    }
}
```

### 2. 만료 UI (FinishedScreen)

**위치**: `ui/tab_01/screens/FinishedScreen.kt`

```kotlin
@Composable
fun FinishedScreen(
    onResultCheck: () -> Unit = {},
    onNewTimerStart: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 완료 아이콘
        Icon(
            imageVector = Icons.Default.CheckCircle,
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Text(text = "목표 달성 완료!", fontSize = 32.sp)
        Text(text = "지금까지 잘 해왔어요!", fontSize = 18.sp)
        
        // 결과 확인 버튼 (광고 노출)
        Button(onClick = onResultCheck) {
            Text("결과 확인")
        }
        
        // 새 타이머 시작 버튼 (상태 해제)
        OutlinedButton(onClick = onNewTimerStart) {
            Text("새 타이머 시작")
        }
    }
}
```

### 3. 네비게이션 로직 (NavGraph.kt)

**위치**: `navigation/NavGraph.kt`

#### A. Run 화면에서 Finished 화면으로 이동

```kotlin
composable(Screen.Run.route) {
    RunScreenComposable(
        onCompletedNavigateToDetail = { route ->
            // [중요] 타이머 완료 시 Finished 화면으로 이동
            Log.d("NavGraph", "타이머 완료 -> Finished 화면으로 이동")
            navController.navigate(Screen.Finished.route) {
                popUpTo(Screen.Run.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
}
```

#### B. Finished 화면 구현

```kotlin
composable(Screen.Finished.route) {
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    FinishedScreen(
        onResultCheck = {
            // [결과 확인] 광고 정책 체크 후 전면 광고 노출
            Log.d("NavGraph", "결과 확인 클릭 -> 광고 정책 체크")
            
            val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
            
            val proceedToDetail: () -> Unit = {
                // 완료된 기록의 상세 화면으로 이동
                try {
                    val completedStartTime = sharedPref.getLong("completed_start_time", 0L)
                    val completedEndTime = sharedPref.getLong("completed_end_time", 0L)
                    // ...기록 정보 가져오기
                    
                    if (completedStartTime > 0 && completedEndTime > 0) {
                        val route = Screen.Detail.createRoute(...)
                        navController.navigate(route)
                    } else {
                        navController.navigate(Screen.Records.route)
                    }
                } catch (t: Throwable) {
                    Log.e("NavGraph", "결과 확인 실패", t)
                    navController.navigate(Screen.Records.route)
                }
                
                // [중요] 만료 상태는 유지 (isFinished = true)
            }
            
            if (shouldShowAd && activity != null) {
                Log.d("NavGraph", "광고 정책 통과 -> 전면 광고 노출")
                if (InterstitialAdManager.isLoaded()) {
                    InterstitialAdManager.show(activity) { success ->
                        Log.d("NavGraph", "광고 결과: $success -> Detail 화면으로 이동")
                        proceedToDetail()
                    }
                } else {
                    proceedToDetail()
                }
            } else {
                proceedToDetail()
            }
        },
        onNewTimerStart = {
            // [새 타이머 시작] 만료 상태 해제 (유일한 해제 경로)
            Log.d("NavGraph", "새 타이머 시작 -> 만료 상태 해제")
            
            // [중요] 만료 상태 해제
            TimerStateRepository.setTimerFinished(false)
            TimerStateRepository.setTimerActive(false)
            
            Log.d("NavGraph", "만료 상태 해제 완료: isFinished=false")
            
            navController.navigate(Screen.Start.route) {
                popUpTo(Screen.Finished.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
}
```

### 4. 광고 정책 관리 (AdPolicyManager)

**위치**: `data/repository/AdPolicyManager.kt`

```kotlin
object AdPolicyManager {
    private const val DEFAULT_INTERSTITIAL_INTERVAL_SECONDS = 1800L // 30분

    fun shouldShowInterstitialAd(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // 1. 쿨타임 간격 가져오기
            val intervalSeconds = getInterstitialIntervalSeconds(context)
            val intervalMillis = intervalSeconds * 1000L
            
            // 2. 마지막 노출 시간 가져오기
            val lastShownTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME_MS, 0L)
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastShownTime
            
            // 3. 쿨타임 검사
            val canShow = elapsedTime >= intervalMillis
            
            if (canShow) {
                // 노출 가능 - 시간 기록
                prefs.edit().putLong(KEY_LAST_INTERSTITIAL_TIME_MS, currentTime).apply()
                Log.d(TAG, "광고 노출 가능 - 마지막 노출 시간 업데이트")
            } else {
                Log.d(TAG, "광고 쿨타임 중: ${elapsedTime/1000}/${intervalSeconds} 초")
            }
            
            return canShow
        } catch (t: Throwable) {
            Log.e(TAG, "shouldShowInterstitialAd 실패", t)
            return false
        }
    }
}
```

---

## ⚠️ 주의 사항

### 1. 상태 해제는 오직 한 곳에서만!

```kotlin
// ✅ 올바른 해제 위치
onNewTimerStart = {
    TimerStateRepository.setTimerFinished(false) // 여기서만!
    navController.navigate(Screen.Start.route)
}

// ❌ 절대 금지!
onResultCheck = {
    TimerStateRepository.setTimerFinished(false) // 금지!
    // ...
}
```

### 2. 결과 확인 시 상태 유지

```kotlin
onResultCheck = {
    // 광고 표시 및 Detail 화면 이동
    // ...
    
    // [중요] 만료 상태는 변경하지 않음!
    // TimerStateRepository.setTimerFinished(false) ← 호출 금지!
}
```

### 3. Detail 화면에서 뒤로가기

Detail 화면에서 뒤로가기를 누르면:
- ✅ 다시 Finished 화면으로 돌아감 (상태 유지 중이므로)
- ✅ 사용자는 '새 타이머 시작' 버튼을 눌러야만 Start 화면으로 이동 가능

---

## 🧪 테스트 시나리오

### 시나리오 1: 정상 플로우

```
1. 타이머 시작 (10초, 테스트 모드)
2. 타이머 만료 대기
3. Finished 화면 표시 확인 ✅
4. '결과 확인' 버튼 클릭
5. 광고 표시 확인 ✅
6. 광고 닫기
7. Detail 화면 이동 확인 ✅
8. 뒤로가기
9. 다시 Finished 화면 표시 확인 ✅ (상태 유지)
10. '새 타이머 시작' 버튼 클릭
11. Start 화면 이동 확인 ✅ (상태 해제)
```

### 시나리오 2: 앱 재시작

```
1. 타이머 만료 상태
2. 앱 강제 종료
3. 앱 재실행
4. 1번째 탭 진입
5. Finished 화면 표시 확인 ✅ (상태 잠금 유지)
```

### 시나리오 3: 다른 탭 왕복

```
1. 타이머 만료 상태 (Finished 화면)
2. 2번째 탭으로 이동
3. 1번째 탭으로 복귀
4. Finished 화면 표시 확인 ✅ (상태 잠금 유지)
```

### 시나리오 4: 광고 쿨타임

```
1. Finished 화면에서 '결과 확인' 클릭
2. 광고 표시 및 Detail 이동
3. 뒤로가기 → Finished 화면
4. 즉시 다시 '결과 확인' 클릭
5. 광고 스킵 (쿨타임 중) ✅
6. Detail 화면 즉시 이동 ✅
```

---

## 📊 상태 다이어그램

```
[정상 상태]
isFinished = false
isActive = false
→ Start 화면

[작동 중 상태]
isFinished = false
isActive = true
→ Run 화면

[만료 상태] ← 상태 잠금!
isFinished = true
isActive = false
→ Finished 화면 (강제)
  ├─ 앱 재시작해도 유지
  ├─ 다른 탭 복귀해도 유지
  └─ '새 타이머 시작'만 해제 가능
```

---

## 🎉 완료 체크리스트

- [x] FinishedScreen 컴포넌트 생성
- [x] NavGraph에 Finished 화면 추가
- [x] '결과 확인' 버튼에 광고 로직 연동
- [x] '새 타이머 시작' 버튼에 상태 해제 로직 추가
- [x] AdPolicyManager 쿨타임 체크 구현
- [x] InterstitialAdManager.show() 함수 활용
- [x] 상태 잠금 유지 확인
- [x] 상태 해제 경로 단일화
- [x] 빌드 성공 확인

---

## 📝 문서 버전

- **작성일**: 2025-12-02
- **최종 업데이트**: 상태 잠금(State Lock) 완전 구현
- **적용 버전**: v1.0.0+

---

## 🔗 관련 문서

- [SEQUENTIAL_EXECUTION_GUIDE.md](./SEQUENTIAL_EXECUTION_GUIDE.md) - 순차적 실행 구조 가이드
- [AD_POLICY_GUIDE.md](./reference/AD_POLICY_GUIDE.md) - 광고 정책 가이드

