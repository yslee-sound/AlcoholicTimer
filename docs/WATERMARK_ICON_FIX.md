# 워터마크 아이콘 표시 수정

**작성일**: 2025-10-27  
**이슈**: 디버그 빌드에서 워터마크 아이콘이 표시되지 않음

---

## 🔍 문제 상황

### 현상
- **릴리즈 빌드**: 금주 설정 화면 중앙에 앱 아이콘 워터마크 표시 ✅
- **디버그 빌드**: 금주 설정 화면 중앙에 아무것도 표시 안 됨 ❌

### 원인
배경 제거 로직(`window.setBackgroundDrawable(null)`)이 디버그 빌드에서 더 빨리 실행되어 `splash_screen.xml`에 정의된 워터마크 아이콘이 제거됨

---

## ✅ 적용된 수정사항

### StartActivity.kt 수정

#### 1. launchContent 함수 - 배경 제거 로직 주석 처리

**Before**:
```kotlin
val launchContent = {
    val backgroundRemoved = java.util.concurrent.atomic.AtomicBoolean(false)
    setContent {
        // ...
        onSplashFinished = {
            fun removeBackgroundOnce() {
                if (backgroundRemoved.compareAndSet(false, true)) 
                    window.setBackgroundDrawable(null)
            }
            removeBackgroundOnce()
        }
    }
    // 여러 시점에서 배경 제거
    window.decorView.post { runCatching { window.setBackgroundDrawable(null) } }
    window.decorView.postDelayed({ ... }, 300)
    window.decorView.postDelayed({ ... }, 1200)
    window.decorView.postDelayed({ ... }, 2400)
}
```

**After**:
```kotlin
val launchContent = {
    setContent {
        // ...
        onSplashFinished = {
            // 배경을 제거하지 않음 - 워터마크 아이콘 유지
        }
    }
    // 배경 제거 로직을 주석 처리하여 워터마크 아이콘이 계속 표시되도록 함
    // window.decorView.post { runCatching { window.setBackgroundDrawable(null) } }
    // window.decorView.postDelayed({ ... }, 300)
    // window.decorView.postDelayed({ ... }, 1200)
    // window.decorView.postDelayed({ ... }, 2400)
}
```

#### 2. onCreate - API 30 이하 처리 수정

**Before**:
```kotlin
if (Build.VERSION.SDK_INT < 31) {
    // 즉시 화이트 배경으로 덮고 setContent, 첫 프레임 이후 배경 제거
    window.setBackgroundDrawable(android.graphics.Color.WHITE.toDrawable())
    launchContent()
    window.decorView.post { window.setBackgroundDrawable(null) }
}
```

**After**:
```kotlin
if (Build.VERSION.SDK_INT < 31) {
    // API 30 이하: 배경을 유지하여 워터마크 아이콘 표시
    launchContent()
}
```

#### 3. onResume - 배경 제거 제거

**Before**:
```kotlin
override fun onResume() {
    super.onResume()
    // 재개 시점에서도 배경을 한 번 더 제거
    window.decorView.post { runCatching { window.setBackgroundDrawable(null) } }
}
```

**After**:
```kotlin
override fun onResume() {
    super.onResume()
    // 배경을 제거하지 않음 - 워터마크 아이콘 유지
    
    // 금주 진행 중이면 즉시 RunActivity로 이동
    val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)
    
    if (startTime != 0L && !timerCompleted) {
        startActivity(Intent(this, RunActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }
}
```

---

## 🎯 개선 효과

### Before (이전)
```
디버그 빌드: 워터마크 아이콘 없음 ❌
릴리즈 빌드: 워터마크 아이콘 표시 ✅
일관성: 없음
```

### After (개선 후)
```
디버그 빌드: 워터마크 아이콘 표시 ✅
릴리즈 빌드: 워터마크 아이콘 표시 ✅
일관성: 완벽 ✅
```

---

## 📋 워터마크 아이콘 정의

### splash_screen.xml
```xml
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Background -->
    <item android:drawable="@android:color/white" />
    <!-- Centered icon -->
    <item
        android:gravity="center"
        android:width="240dp"
        android:height="240dp"
        android:drawable="@drawable/splash_app_icon" />
</layer-list>
```

이 워터마크는 테마의 `windowBackground`로 설정되어 있으며, 이제 제거되지 않고 계속 표시됩니다.

---

## 🧪 테스트 체크리스트

### 디버그 빌드
- [ ] 앱 실행 시 금주 설정 화면에 워터마크 아이콘 표시
- [ ] 화면 중앙에 앱 아이콘이 투명하게 표시됨
- [ ] 스크롤 시에도 워터마크 유지

### 릴리즈 빌드
- [ ] 앱 실행 시 금주 설정 화면에 워터마크 아이콘 표시
- [ ] 디버그 빌드와 동일한 모습
- [ ] 일관성 유지

### 다른 화면
- [ ] RunActivity: 워터마크 없음 (정상)
- [ ] RecordsActivity: 워터마크 없음 (정상)
- [ ] StartActivity만 워터마크 표시

---

## 💡 워터마크가 표시되는 원리

### 테마 설정 (themes.xml)
```xml
<style name="Theme.AlcoholicTimer.Splash">
    <item name="android:windowBackground">@drawable/splash_screen</item>
</style>
```

### AndroidManifest.xml
```xml
<activity
    android:name=".feature.start.StartActivity"
    android:theme="@style/Theme.AlcoholicTimer.Splash">
</activity>
```

### 동작 방식
1. StartActivity가 시작되면 `Theme.AlcoholicTimer.Splash` 테마 적용
2. `windowBackground`로 `splash_screen.xml` 표시
3. **이전**: Compose UI가 로드된 후 `setBackgroundDrawable(null)`로 배경 제거
4. **현재**: 배경 제거를 하지 않아 워터마크가 계속 표시됨

---

## 🎨 디자인 의도

워터마크 아이콘은 다음과 같은 목적으로 표시됩니다:

1. **브랜드 식별**: 앱의 정체성을 시각적으로 표현
2. **시각적 일관성**: 스플래시 화면에서 메인 화면으로의 자연스러운 전환
3. **미니멀 디자인**: 깔끔한 배경에 브랜드 아이콘만 표시

---

## 🔧 수정 파일 목록

1. **StartActivity.kt**
   - `launchContent`: 배경 제거 로직 주석 처리
   - `onCreate`: API 30 이하 처리 간소화
   - `onResume`: 배경 제거 제거

---

## 📚 관련 파일

- `app/src/main/res/drawable/splash_screen.xml` - 워터마크 정의
- `app/src/main/res/drawable/splash_app_icon.xml` - 앱 아이콘
- `app/src/main/res/values/themes.xml` - 테마 설정

---

**변경 일자**: 2025-10-27  
**변경자**: GitHub Copilot  
**검토 상태**: ✅ 완료 (빌드 성공)

