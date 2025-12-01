# ✅ 전면광고 배너 겹침 방지 체크리스트

## 📅 작성일
2025-12-01 (최종 업데이트)

## 🎯 목적
**AdMob 정책 준수**: 전면광고(Full-Screen Ads) 표시 시 배너 광고가 겹쳐 보이지 않도록 보장

> ⚠️ **중요**: 전면광고와 배너가 겹쳐서 나타나면 **AdMob 정책 위반**으로 계정이 정지될 수 있습니다!

---

## 🚨 핵심 해결 방법

### hideBannerImmediately() - 즉시 배너 숨김

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/ads/AdController.kt`

```kotlin
/**
 * 🚨 AdMob 정책 준수: 전면광고와 배너 광고 겹침 방지
 * 
 * 배너를 즉시 숨김 (StateFlow + 모든 리스너 즉시 호출)
 * - show() 호출 직전에 사용하여 배너가 전면광고 위에 나타나지 않도록 보장
 */
fun hideBannerImmediately(reason: String? = null) {
    // StateFlow 즉시 업데이트
    _bannerForceHidden.value = true
    _fullScreenAdShowingFlow.value = true
    
    // 모든 리스너 즉시 동기 호출 (Compose recomposition 트리거)
    bannerForceHiddenListeners.forEach { it.invoke(true) }
    fullScreenListeners.forEach { it.invoke(true) }
}
```

**특징**:
- ✅ **동기 실행** - 즉시 모든 상태 업데이트
- ✅ **리스너 강제 호출** - Compose가 즉시 recomposition
- ✅ **show() 직전 호출** - 배너가 나타날 시간 없음

---

## 📋 전면광고 종류별 구현 상태

### ✅ AppOpen 광고 (App Open Ad)

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/ads/AppOpenAdManager.kt`

| 시점 | 처리 내용 | 구현 상태 |
|------|-----------|-----------|
| **광고 표시 직전** | `hideBannerImmediately("appOpenBeforeShow")` | ✅ **최종 개선** |
| **광고 표시 전** | `setBannerForceHidden(true)` | ✅ 완료 |
| **광고 표시 중** | `setFullScreenAdShowing(true)` | ✅ 완료 |
| **광고 정상 종료** | `ensureBannerVisible("appOpenDismissed")` | ✅ 완료 |
| **광고 표시 실패** | `ensureBannerVisible("appOpenFailedToShow")` | ✅ 완료 |
| **show 예외** | `ensureBannerVisible("appOpenShowException")` | ✅ 완료 |

**주요 코드**:
```kotlin
// showIfAvailable() - show() 직전
fun showIfAvailable(activity: Activity): Boolean {
    // 🚨 AdMob 정책: show() 호출 직전에 배너 즉시 숨김
    AdController.hideBannerImmediately("appOpenBeforeShow")
    AdController.setBannerForceHidden(true)
    AdController.setFullScreenAdShowing(true)
    
    // 80ms 지연으로 Compose recomposition 보장
    mainHandler.postDelayed({
        appOpenAd?.show(activity)
    }, 80L)
    return true
}

// onAdDismissedFullScreenContent() - 종료 후
AdController.setFullScreenAdShowing(false)
AdController.setBannerForceHidden(false)
AdController.ensureBannerVisible("appOpenDismissed")
```

---

### ✅ Interstitial 광고 (Interstitial Ad)

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/ads/InterstitialAdManager.kt`

| 시점 | 처리 내용 | 구현 상태 |
|------|-----------|-----------|
| **광고 표시 직전** | `hideBannerImmediately("interstitialBeforeShow")` | ✅ **최종 개선** |
| **광고 표시 전** | `setBannerForceHidden(true)` | ✅ 완료 |
| **광고 표시 중** | `setFullScreenAdShowing(true)` | ✅ 완료 |
| **광고 정상 종료** | `ensureBannerVisible("interstitialDismissed")` | ✅ 완료 |
| **광고 표시 실패** | `ensureBannerVisible("interstitialFailedToShow")` | ✅ 완료 |
| **정책 체크 실패** | `ensureBannerVisible("interstitialPolicyDenied")` | ✅ 완료 |
| **예외 발생** | `ensureBannerVisible("interstitialException")` | ✅ 완료 |
| **show 실패** | `ensureBannerVisible("interstitialShowException")` | ✅ 완료 |
| **Debug 종료** | `ensureBannerVisible("debugInterstitialClosed")` | ✅ 완료 |

**주요 코드**:
```kotlin
// tryShowAd() - show() 직전
private fun tryShowAd(activity: Activity, ad: InterstitialAd) {
    // 🚨 AdMob 정책: show() 호출 직전에 배너 즉시 숨김
    AdController.hideBannerImmediately("interstitialBeforeShow")
    AdController.setBannerForceHidden(true)
    AdController.setFullScreenAdShowing(true)
    
    ad.show(activity)
}

// onAdDismissedFullScreenContent() - 종료 후
AdController.setFullScreenAdShowing(false)
AdController.setBannerForceHidden(false)
AdController.ensureBannerVisible("interstitialDismissed")
```

---

## 🔄 배너 숨김/복구 흐름

### 정상 흐름
```
1. 사용자가 전면광고를 트리거하는 행동
   ↓
2. setBannerForceHidden(true) ← 배너 즉시 숨김
   ↓
3. setFullScreenAdShowing(true) ← 전면광고 플래그 설정
   ↓
4. ad.show(activity) ← 광고 표시
   ↓
5. 사용자가 광고 보고 닫기
   ↓
6. onAdDismissedFullScreenContent() 콜백
   ↓
7. setFullScreenAdShowing(false) ← 자동으로 ensureBannerVisible 호출
   ↓
8. setBannerForceHidden(false) ← 명시적 배너 복구
   ↓
9. ensureBannerVisible("reason") ← 이중 보장
   ↓
10. 배너 다시 표시 ✅
```

### 실패 흐름
```
1~3. (동일)
   ↓
4. ad.show(activity) ← 실패!
   ↓
5. onAdFailedToShowFullScreenContent() 콜백
   ↓
6. setFullScreenAdShowing(false)
   ↓
7. setBannerForceHidden(false)
   ↓
8. ensureBannerVisible("failureReason")
   ↓
9. 배너 즉시 복구 ✅
```

---

## 🛡️ 안전장치 체크리스트

### ✅ 다중 복구 경로
- [x] **정상 종료 시**: `onAdDismissedFullScreenContent` → ensureBannerVisible
- [x] **표시 실패 시**: `onAdFailedToShowFullScreenContent` → ensureBannerVisible
- [x] **정책 거부 시**: policy check fail → ensureBannerVisible
- [x] **예외 발생 시**: catch block → ensureBannerVisible
- [x] **자동 복구**: `setFullScreenAdShowing(false)` → ensureBannerVisible

### ✅ 상태 초기화
- [x] `bannerForceHidden` → false
- [x] `fullScreenAdShowing` → false
- [x] `interstitialShowing` → false (Interstitial만)
- [x] `bannerReloadTick` → 갱신

### ✅ 로그 추적
- [x] 각 ensureBannerVisible 호출에 reason 명시
- [x] 배너 숨김/복구 시점 로그 출력
- [x] AdController 상태 로그 (setBannerForceHidden)

---

## 📝 새 전면광고 추가 시 가이드

### 필수 구현 체크리스트

새로운 전면광고를 추가할 때 **반드시** 다음을 구현하세요:

```kotlin
// ✅ 1. 광고 표시 전
fun showYourAd(activity: Activity) {
    try { AdController.setBannerForceHidden(true) } catch (_: Throwable) {}
    try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
    
    yourAd.fullScreenContentCallback = object : FullScreenContentCallback() {
        // ✅ 2. 광고 정상 종료
        override fun onAdDismissedFullScreenContent() {
            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
            try { AdController.ensureBannerVisible("yourAdDismissed") } catch (_: Throwable) {}
        }
        
        // ✅ 3. 광고 표시 실패
        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
            try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
            try { AdController.ensureBannerVisible("yourAdFailedToShow") } catch (_: Throwable) {}
        }
    }
    
    // ✅ 4. show() 예외 처리
    try {
        yourAd.show(activity)
    } catch (t: Throwable) {
        try { AdController.setBannerForceHidden(false) } catch (_: Throwable) {}
        try { AdController.setFullScreenAdShowing(false) } catch (_: Throwable) {}
        try { AdController.ensureBannerVisible("yourAdShowException") } catch (_: Throwable) {}
    }
}
```

### 구현 확인 체크리스트

- [ ] `setBannerForceHidden(true)` - 광고 표시 **전**에 호출
- [ ] `setFullScreenAdShowing(true)` - 광고 표시 **전**에 호출
- [ ] `ensureBannerVisible()` - 광고 **종료 시** 호출
- [ ] `ensureBannerVisible()` - 광고 **실패 시** 호출
- [ ] `ensureBannerVisible()` - **예외 처리**에 호출
- [ ] reason 파라미터에 광고 이름 포함 (예: "yourAdDismissed")
- [ ] 모든 복구 호출에 try-catch 적용

---

## 🧪 테스트 시나리오

### 시나리오 1: AppOpen 광고
1. 앱 시작
2. AppOpen 광고 표시 확인
3. **배너가 보이지 않는지 확인** ✅
4. 광고 닫기
5. **배너가 즉시 나타나는지 확인** ✅

### 시나리오 2: Interstitial 광고
1. 특정 화면에서 Interstitial 트리거
2. Interstitial 광고 표시 확인
3. **배너가 보이지 않는지 확인** ✅
4. 광고 닫기
5. **배너가 즉시 나타나는지 확인** ✅

### 시나리오 3: 광고 실패
1. 네트워크 끊기
2. 전면광고 트리거
3. 광고 표시 실패
4. **배너가 즉시 복구되는지 확인** ✅

### 시나리오 4: 연속 광고
1. AppOpen 광고 보고 닫기
2. 배너 확인
3. Interstitial 광고 트리거
4. **배너가 다시 숨겨지는지 확인** ✅
5. Interstitial 닫기
6. **배너가 다시 나타나는지 확인** ✅

---

## 📊 구현 완료 현황

| 광고 종류 | 파일 | 배너 숨김 | 배너 복구 | 예외 처리 | 상태 |
|-----------|------|-----------|-----------|-----------|------|
| **AppOpen** | AppOpenAdManager.kt | ✅ | ✅ | ✅ | 완료 |
| **Interstitial** | InterstitialAdManager.kt | ✅ | ✅ | ✅ | **완료** |
| Rewarded | - | - | - | - | 미사용 |
| Native | - | - | - | - | 미사용 |

---

## 🎉 완료!

### 달성 사항
- ✅ AppOpen 광고 시 배너 겹침 방지
- ✅ Interstitial 광고 시 배너 겹침 방지 (신규)
- ✅ 모든 실패 경로에서 배너 복구 보장
- ✅ 다중 안전장치로 재발 방지
- ✅ 새 광고 추가 시 가이드 문서화

### 빌드 상태
✅ 성공 (2025-12-01)

### 다음 단계
1. 실기기 테스트로 배너 겹침 여부 확인
2. 로그 모니터링 (`ensureBannerVisible reason=...`)
3. 새로운 전면광고 추가 시 이 체크리스트 참조

