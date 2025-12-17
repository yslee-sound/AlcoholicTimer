# 전면광고(Interstitial Ad) 노출 타이밍 분석 보고서

**분석일**: 2025-12-17  
**분석 대상**: AlcoholicTimer 앱의 모든 전면광고 표시 지점

---

## 🎯 전면광고가 나오는 타이밍 (총 4곳)

### 1️⃣ 타이머 완료 후 "결과 확인" 버튼 클릭 시 ⭐ 핵심

**위치**: `ui/main/AppNavHost.kt` (DetailScreen)

**시나리오**:
```
타이머 만료됨
  ↓
"결과 확인" 버튼 클릭
  ↓
전면광고 표시 ✅
  ↓
광고 종료 후 → 기록 화면으로 이동
```

**조건**:
- `AdPolicyManager.shouldShowInterstitialAd()` = true (쿨타임 통과)
- `InterstitialAdManager.isLoaded()` = true (광고 로드됨)

**코드**:
```kotlin
// AppNavHost.kt Line 155-177
onResultConfirm = {
    val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
    
    val proceedToDetail: () -> Unit = {
        // 기록 화면으로 이동
        navController.navigate(Screen.Records.route)
    }
    
    if (shouldShowAd && activity != null) {
        if (InterstitialAdManager.isLoaded()) {
            InterstitialAdManager.show(activity) { 
                proceedToDetail() 
            }
        } else {
            proceedToDetail()
        }
    } else {
        proceedToDetail()
    }
}
```

---

### 2️⃣ "전체 기록" 화면 뒤로가기 시

**위치**: `ui/main/navigation/Tab02DetailGraph.kt` (AllRecordsScreen)

**시나리오**:
```
Tab 2 (통계) → "전체 기록" 화면 진입
  ↓
사용자가 뒤로가기 버튼 클릭
  ↓
전면광고 표시 ✅
  ↓
광고 종료 후 → 이전 화면으로 복귀
```

**조건**:
- `shouldShowInterstitialAd()` = true
- `InterstitialAdManager.isLoaded()` = true

**코드**:
```kotlin
// Tab02DetailGraph.kt Line 200-228
AllRecordsScreen(
    onNavigateBack = {
        val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
        
        val proceedBack: () -> Unit = {
            navController.popBackStack()
        }
        
        if (shouldShowAd && activity != null) {
            if (InterstitialAdManager.isLoaded()) {
                InterstitialAdManager.show(activity) { _ ->
                    proceedBack()
                }
            } else {
                proceedBack()
            }
        } else {
            proceedBack()
        }
    }
)
```

---

### 3️⃣ "전체 일기" 화면 뒤로가기 시

**위치**: `ui/main/navigation/Tab02DetailGraph.kt` (AllDiaryScreen)

**시나리오**:
```
Tab 2 (통계) → "전체 일기" 화면 진입
  ↓
사용자가 뒤로가기 버튼 클릭
  ↓
전면광고 표시 ✅
  ↓
광고 종료 후 → 이전 화면으로 복귀
```

**조건**:
- `shouldShowInterstitialAd()` = true
- `InterstitialAdManager.isLoaded()` = true

**코드**:
```kotlin
// Tab02DetailGraph.kt Line 270-300
AllDiaryScreen(
    onNavigateBack = {
        val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
        
        val proceedBack: () -> Unit = {
            navController.popBackStack()
        }
        
        if (shouldShowAd && activity != null) {
            if (InterstitialAdManager.isLoaded()) {
                InterstitialAdManager.show(activity) { _ ->
                    proceedBack()
                }
            } else {
                proceedBack()
            }
        } else {
            proceedBack()
        }
    }
)
```

---

### 4️⃣ MainActivity의 테스트 함수 (실제 사용 안 됨)

**위치**: `ui/main/MainActivity.kt`

**설명**: 
- `showResultAndRecord()` 함수가 있지만 `@Suppress("unused")` 표시
- 실제로는 사용되지 않는 테스트/디버그용 코드
- 위의 1번 케이스가 실제 구현임

---

## 📊 광고 표시 조건 (공통)

### 1. 쿨타임 체크 (AdPolicyManager)

```kotlin
fun shouldShowInterstitialAd(context: Context): Boolean
```

**체크 항목**:
1. **쿨타임 간격 조회**
   - Firebase Remote Config: `interstitial_interval_sec`
   - DEBUG 모드: 커스텀 설정 가능
   - 기본값: 300초 (5분)

2. **마지막 노출 시간 확인**
   - SharedPreferences: `last_ad_shown_time_ms`
   - 전면광고 + 앱오프닝 통합 관리

3. **경과 시간 계산**
   ```kotlin
   val elapsedTime = currentTime - lastShownTime
   val canShow = elapsedTime >= intervalMillis
   ```

4. **결과 반환**
   - `true`: 광고 노출 가능 (쿨타임 경과) → 마지막 노출 시간 업데이트
   - `false`: 쿨타임 중 (광고 스킵)

### 2. 광고 로드 상태 체크

```kotlin
InterstitialAdManager.isLoaded()
```

**체크 항목**:
- 광고가 미리 로드되어 있는지 확인
- `false`면 광고를 건너뛰고 다음 화면으로 진행

---

## 🕐 실제 사용자 경험 타임라인

### 시나리오 A: 타이머 완료 (가장 일반적)

```
08:00 - 타이머 시작
10:00 - 타이머 완료 (2시간)
        "결과 확인" 버튼 표시
        
10:05 - 사용자가 "결과 확인" 클릭
        ↓
        쿨타임 체크: 통과 (첫 광고)
        광고 로드 체크: 통과
        ↓
        전면광고 표시 ✅ (30초)
        ↓
        기록 화면으로 이동
        
10:06 - 사용자가 앱 종료

10:30 - 앱 재실행 → 앱 오프닝 광고 표시
        (쿨타임 통합 관리로 가능)
```

### 시나리오 B: "전체 기록" 둘러보기

```
14:00 - Tab 2 (통계) 진입
14:01 - "전체 기록" 화면 진입
14:02 - 기록 확인 후 뒤로가기 클릭
        ↓
        쿨타임 체크: 통과 (5분 이상 경과)
        광고 로드 체크: 통과
        ↓
        전면광고 표시 ✅
        ↓
        Tab 2로 복귀
```

### 시나리오 C: 쿨타임 중 (광고 스킵)

```
15:00 - 타이머 완료 → "결과 확인" 클릭
        → 전면광고 표시 ✅
        → 마지막 노출 시간: 15:00
        
15:03 - "전체 기록" 진입 후 뒤로가기
        ↓
        쿨타임 체크: 실패 (3분 < 5분)
        ↓
        광고 스킵 (즉시 복귀) ✅
```

---

## 🎮 광고 정책 설정

### Firebase Remote Config

**키**: `interstitial_interval_sec`

**기본값**:
- Release: 300초 (5분)
- Debug: 60초 (1분) 또는 커스텀 설정

**설정 방법**:
```
Firebase Console
→ Remote Config
→ "interstitial_interval_sec" 수정
→ 앱 재시작 시 자동 적용
```

### Debug 모드 커스텀 설정

**위치**: Tab 5 → 디버그 메뉴

**설정 가능**:
- 쿨타임 간격 변경 (초 단위)
- 광고 강제 비활성화
- 쿨타임 무시

---

## 📈 광고 빈도 분석

### 일반 사용자 (하루 기준)

**타이머 사용 패턴**:
```
09:00 - 타이머 시작
11:00 - 타이머 완료 → 광고 1회 ✅
15:00 - 타이머 시작
18:00 - 타이머 완료 → 광고 2회 ✅
21:00 - 기록 둘러보기 → 광고 3회 ✅
```

**예상 노출 횟수**: 2~4회/일

### 활동적인 사용자

**추가 노출 지점**:
- 타이머 완료: 3~5회
- 전체 기록 탐색: 1~2회
- 전체 일기 탐색: 1~2회

**예상 노출 횟수**: 5~9회/일 (쿨타임으로 제한됨)

---

## 🔧 광고 로드 전략

### 사전 로드 (Preload)

**시점**:
1. 앱 시작 시 (MainActivity.onCreate)
2. 광고 표시 후 자동 재로드

**코드**:
```kotlin
// InterstitialAdManager.kt
override fun onAdDismissedFullScreenContent() {
    // 광고 종료 후 자동으로 다음 광고 미리 로드
    val context = activity.applicationContext
    loadInterstitial(context)
}
```

### 로드 실패 시

**처리**:
- 광고를 건너뛰고 정상 진행
- 다음 기회에 다시 로드 시도
- 앱 흐름에 영향 없음

---

## ⚠️ 주의사항

### 1. 쿨타임 통합 관리

전면광고와 앱 오프닝 광고가 **같은 쿨타임을 공유**합니다.

**SharedPreferences 키**: `last_ad_shown_time_ms`

**의미**:
- 전면광고를 본 후 5분 내에는 앱 오프닝 광고도 안 나옴
- 반대의 경우도 동일

### 2. AdMob 빈도 제한

AdMob에서 설정한 빈도 제한이 추가로 적용될 수 있습니다.

**우선순위**:
1. AdMob 빈도 제한 (서버 측)
2. Firebase 쿨타임 (앱 측)
3. **더 엄격한 제한이 적용됨**

### 3. 광고 없이도 정상 작동

광고가 로드되지 않았거나 쿨타임 중이어도:
- ✅ 앱은 정상 작동
- ✅ 화면 전환 정상 진행
- ✅ 사용자 경험 유지

---

## 📊 요약

### 전면광고 노출 지점 (3곳)

1. ⭐ **타이머 완료 → "결과 확인"** (가장 중요)
2. **"전체 기록" → 뒤로가기**
3. **"전체 일기" → 뒤로가기**

### 공통 조건

1. ✅ 쿨타임 경과 (기본 5분)
2. ✅ 광고 로드됨
3. ✅ Activity 존재

### 사용자 경험

- **자연스러운 타이밍**: 화면 전환 전에 노출
- **쿨타임으로 제한**: 너무 자주 나오지 않음
- **스킵 가능**: 조건 불충족 시 즉시 진행

---

**작성일**: 2025-12-17  
**분석자**: GitHub Copilot  
**파일 수**: 4개 (MainActivity, AppNavHost, Tab02DetailGraph x2)

