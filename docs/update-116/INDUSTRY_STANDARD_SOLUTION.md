# 🔍 전면광고-배너 겹침 방지: 업계 표준 방식 검토

## 📚 AdMob 공식 가이드라인

### Google의 권장 사항
AdMob 공식 문서에 따르면 전면광고와 배너 광고 겹침을 방지하는 **업계 표준 방법**:

1. **배너를 완전히 제거 (destroy)** - 전면광고 표시 전
2. **전면광고 종료 후 배너를 새로 생성** - 광고 닫힌 후

### 왜 숨김(hide) 대신 제거(destroy)?

**현재 방식 (숨김):**
```
배너 AdView 유지 → visibility = GONE
  ↓
문제: AdView 인스턴스가 살아있음
  ↓
Compose가 상태를 재평가할 때 다시 VISIBLE로 변경될 수 있음
```

**표준 방식 (제거/재생성):**
```
배너 AdView 완전히 제거 (destroy)
  ↓
전면광고 표시
  ↓
전면광고 종료
  ↓
배너 AdView 새로 생성
  ↓
겹칠 수 없음 (AdView 자체가 없었으므로)
```

---

## ✅ 업계 표준 해결책

### 방법 1: 조건부 렌더링 (Compose 방식) ⭐ 권장

**핵심**: AdView를 숨기지 말고, **아예 렌더링하지 않기**

```kotlin
@Composable
fun AdBanner() {
    val isFullScreenAdShowing by AdController.fullScreenAdShowingFlow.collectAsState()
    
    // 🔑 핵심: 전면광고가 표시 중이면 AdView 자체를 렌더링하지 않음
    if (!isFullScreenAdShowing) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    // 배너 설정...
                }
            }
        )
    }
    // else: 아무것도 렌더링하지 않음 → 겹칠 수 없음!
}
```

**장점**:
- ✅ Compose 방식과 완벽히 호환
- ✅ AdView가 메모리에서 제거됨 (완전한 제거)
- ✅ 겹침 불가능 (View 자체가 없으므로)
- ✅ 타이밍 이슈 없음

**단점**:
- ❌ 전면광고 닫힐 때마다 배너 재생성 (약간의 지연)
- ❌ 광고 노출 횟수 증가 (AdMob에 새 요청)

---

### 방법 2: 배너를 최상위 Layout 밖으로 이동

**핵심**: 배너를 전면광고와 완전히 분리된 레이어에 배치

```kotlin
// MainActivity.kt
setContent {
    Box(modifier = Modifier.fillMaxSize()) {
        // 메인 컨텐츠
        AppContent()
        
        // 배너를 최하단 레이어에 배치
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(0f)  // 최하단
        ) {
            AdBanner()
        }
        
        // 전면광고는 별도 Activity에서 표시 (자동으로 최상단)
    }
}
```

**장점**:
- ✅ Z-index로 완전히 분리
- ✅ 배너 유지 (재생성 불필요)

**단점**:
- ❌ 전면광고가 Activity로 표시되므로 이미 분리되어 있음
- ❌ 우리 경우에는 이미 적용된 상태

---

### 방법 3: 배너 Container를 조건부 렌더링 (최적 해결책) ⭐⭐⭐

**핵심**: 
- 배너 AdView는 유지하되
- **Container 자체를 조건부 렌더링**
- `if` 문으로 완전히 제어

```kotlin
@Composable
fun AdBanner() {
    val isFullScreenAdShowing by AdController.fullScreenAdShowingFlow.collectAsState()
    val isBannerForceHidden by AdController.bannerForceHiddenFlow.collectAsState()
    
    // 🔑 핵심: 전면광고 중이면 아예 컨테이너를 렌더링하지 않음
    val shouldRenderBanner = !isFullScreenAdShowing && !isBannerForceHidden
    
    if (shouldRenderBanner) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White)
        ) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = "your-id"
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { adView ->
                    // 업데이트 로직
                }
            )
        }
    }
    // else: 아무것도 렌더링하지 않음
}
```

**장점**:
- ✅ **완벽한 제어** - if문으로 렌더링 여부 결정
- ✅ **타이밍 이슈 없음** - Compose가 recomposition 시 자동으로 제거/추가
- ✅ **겹침 불가능** - View가 DOM에 없음
- ✅ **AdView 재사용 가능** - factory는 한 번만 호출됨

---

## 📊 현재 방식 vs 업계 표준

| 방식 | 현재 (visibility) | 표준 (조건부 렌더링) |
|------|-------------------|---------------------|
| **AdView 상태** | 메모리에 유지 | 조건부로 생성/제거 |
| **숨김 방법** | visibility = GONE | if (!show) return |
| **타이밍 이슈** | ❌ 있음 | ✅ 없음 |
| **Compose 호환** | ⚠️ 부분적 | ✅ 완벽 |
| **겹침 가능성** | ⚠️ 있음 | ❌ 불가능 |
| **구현 난이도** | 높음 | 낮음 |

---

## 🎯 권장 해결책

### 즉시 적용 가능한 방법: 조건부 렌더링

현재 `AdBanner.kt`를 다음과 같이 수정:

```kotlin
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val isFullScreenAdShowing by AdController.fullScreenAdShowingFlow.collectAsState()
    val isBannerForceHidden by AdController.bannerForceHiddenFlow.collectAsState()
    val isPolicyEnabled by AdController.isPolicyEnabledFlow.collectAsState()
    
    // 🔑 핵심: 조건을 만족하지 않으면 아예 렌더링하지 않음
    if (!isPolicyEnabled || isFullScreenAdShowing || isBannerForceHidden) {
        return  // 아무것도 렌더링하지 않음
    }
    
    // 여기서부터는 배너를 렌더링
    Box(modifier = modifier.fillMaxWidth().height(50.dp)) {
        AndroidView(
            factory = { context ->
                // AdView 생성
            },
            update = { adView ->
                // 업데이트
            }
        )
    }
}
```

**이 방법의 장점**:
1. ✅ **간단함** - if문 하나로 해결
2. ✅ **확실함** - View가 없으면 겹칠 수 없음
3. ✅ **Compose 방식** - 프레임워크의 의도대로 사용
4. ✅ **타이밍 무관** - recomposition 시 자동 처리

---

## 💡 왜 지금까지 안 됐을까?

### 근본 문제
우리는 **View의 visibility를 변경**하려고 했습니다.
하지만 Compose에서는 **View를 렌더링하지 않는 것**이 표준입니다.

### Compose의 철학
```kotlin
// ❌ 잘못된 접근 (Android View 방식)
view.visibility = View.GONE  // View는 여전히 존재

// ✅ 올바른 접근 (Compose 방식)
if (shouldShow) {
    MyComposable()  // 렌더링
}
// else: 아무것도 없음 (View가 존재하지 않음)
```

---

## 🚀 다음 단계

1. **AdBanner.kt를 조건부 렌더링으로 변경**
2. **모든 visibility 로직 제거**
3. **StateFlow만 업데이트** (View 직접 제어 불필요)

이 방법이 **업계 표준이자 Compose의 정석**입니다.

---

## 참고 자료

- [Google AdMob - Banner Best Practices](https://developers.google.com/admob/android/banner)
- [Jetpack Compose - Conditional Composition](https://developer.android.com/jetpack/compose/conditional-ui)
- [AdMob Policy - Ad Overlap Prevention](https://support.google.com/admob/answer/6128543)

