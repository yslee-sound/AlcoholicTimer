# 스플래시 화면 로고 크기 수정 가이드

## 문제 상황

Android 기기별로 스플래시 화면의 로고 크기가 다르게 표시되는 문제가 발생했습니다.

### 증상
- **Pixel 7 (Android 12+)**: 로고가 적절한 크기로 표시됨
- **Pixel 4a (Android 11 이하)**: 로고가 상대적으로 크게 표시됨 (또는 작게 표시될 수도 있음)

### 원인 분석

Android 12 (API 31)부터 스플래시 스크린 시스템이 완전히 변경되어, 동일한 설정이 각 버전에서 다르게 렌더링됩니다.

#### 1. Android 12+ (Pixel 7 등)
- **새로운 SplashScreen API** 사용
- `values-v31/themes.xml`의 설정 적용
- 주요 속성:
  - `windowSplashScreenAnimatedIcon`: 스플래시 아이콘 지정
  - `windowSplashScreenBackground`: 배경색
- **시스템이 자동으로 아이콘 크기를 조정**
  - 아이콘을 약 240dp 직경의 원형 영역에 맞춰 자동 스케일링
  - 개발자가 지정한 크기와 무관하게 시스템이 비율 조정

#### 2. Android 11 이하 (Pixel 4a 등)
- **기존 windowBackground 방식** 사용
- `values/themes.xml`의 설정 적용
- `windowBackground`에 drawable 리소스 지정
- **개발자가 지정한 크기가 그대로 표시됨**
  - `splash_background.xml`에 설정된 width/height 값이 직접 적용
  - 시스템의 자동 스케일링 없음

#### 3. 크기 차이 발생 이유
```
동일한 240dp 설정 시:
- Android 12+: 시스템이 240dp를 기준으로 자동 축소/확대 → 실제 약 200dp 정도로 보임
- Android 11 이하: 240dp가 그대로 표시 → 실제 240dp로 보임
→ 결과적으로 Android 11 이하에서 더 크게 보임
```

## 해결 방법

### 파일 구조
```
app/src/main/res/
├── drawable/
│   ├── ic_splash_logo.xml          # 스플래시 로고 벡터 이미지
│   └── splash_background.xml       # Android 11 이하용 스플래시 배경
├── values/
│   └── themes.xml                  # Android 11 이하용 테마
└── values-v31/
    └── themes.xml                  # Android 12+ 용 테마
```

### 1. Android 12+ 설정 (values-v31/themes.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.PocketChord" parent="android:Theme.Material.Light.NoActionBar">
        <!-- 배경색 -->
        <item name="android:windowSplashScreenBackground">#f28090</item>
        
        <!-- 애니메이티드 아이콘 (240dp 권장) -->
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_splash_logo</item>
        
        <!-- 아이콘 배경색 (투명) -->
        <item name="android:windowSplashScreenIconBackgroundColor">@android:color/transparent</item>
        
        <!-- 스플래시 종료 애니메이션 지속시간 -->
        <item name="android:windowSplashScreenAnimationDuration">200</item>
        
        <!-- 시스템 바 색상 -->
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:statusBarColor">#f28090</item>
        <item name="android:navigationBarColor">#f28090</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>
</resources>
```

**참고**: `ic_splash_logo.xml`의 크기는 240dp x 240dp로 설정되어 있지만, 시스템이 자동으로 조정하므로 실제 표시 크기는 다를 수 있습니다.

### 2. Android 11 이하 설정 (drawable/splash_background.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Solid background color (#f28090) -->
    <item android:drawable="@color/brand_pink"/>

    <!-- Centered app logo (vector drawable) -->
    <item
        android:gravity="center"
        android:width="280dp"
        android:height="280dp"
        android:drawable="@drawable/ic_splash_logo"/>
</layer-list>
```

**핵심**: Android 12+에서 보이는 크기와 동일하게 만들려면 **280dp**로 설정합니다.

### 3. Android 11 이하 테마 (values/themes.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.PocketChord" parent="android:Theme.Material.Light.NoActionBar" />

    <style name="Theme.PocketChord.Launch" parent="Theme.PocketChord">
        <!-- windowBackground를 스플래시 배경으로 사용 -->
        <item name="android:windowBackground">@drawable/splash_background</item>
        
        <!-- 시스템 바 색상 -->
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:statusBarColor">#f28090</item>
        <item name="android:navigationBarColor">#f28090</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>
</resources>
```

### 4. AndroidManifest.xml 설정

```xml
<application
    android:theme="@style/Theme.PocketChord"
    ...>
    <activity
        android:name=".MainActivity"
        android:theme="@style/Theme.PocketChord.Launch"
        ...>
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

**중요**: MainActivity에 `Theme.PocketChord.Launch` 테마를 적용합니다.

### 5. MainActivity에서 테마 전환

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 스플래시 스크린 후 일반 테마로 전환
        setTheme(R.style.Theme_PocketChord)
        
        setContent {
            // ...
        }
    }
}
```

## 크기 조정 가이드

### 크기 결정 방법

1. **기준 기기 선택**: Android 12+ 기기를 기준으로 설정
2. **Android 12+ 크기 설정**: `ic_splash_logo.xml`을 240dp로 유지
3. **Android 11 이하 크기 조정**: 다음 비율로 계산

```
Android 11 이하 크기 = Android 12+ 크기 × 약 1.16
예: 240dp × 1.16 ≈ 280dp
```

### 실제 테스트 절차

1. Android 12+ 기기에서 먼저 테스트하여 원하는 크기 확인
2. `ic_splash_logo.xml`의 크기를 그대로 유지 (예: 240dp)
3. `splash_background.xml`의 크기를 더 크게 설정 (예: 280dp)
4. Android 11 이하 기기에서 테스트
5. 크기가 맞지 않으면 `splash_background.xml`의 크기만 조정:
   - 너무 작으면: 크기를 증가 (예: 280dp → 300dp)
   - 너무 크면: 크기를 감소 (예: 280dp → 260dp)

## 다른 프로젝트에 적용하기

### 체크리스트

- [ ] `drawable/ic_splash_logo.xml` 파일 확인 (240dp x 240dp 권장)
- [ ] `drawable/splash_background.xml` 생성 및 로고 크기 설정 (280dp x 280dp)
- [ ] `values/themes.xml`에 `Theme.YourApp.Launch` 스타일 추가
- [ ] `values-v31/themes.xml`에 Android 12+ 스플래시 속성 추가
- [ ] `AndroidManifest.xml`에서 MainActivity 테마를 `Theme.YourApp.Launch`로 설정
- [ ] `MainActivity.onCreate()`에서 `setTheme()`로 일반 테마로 전환
- [ ] Android 12+ 기기에서 테스트
- [ ] Android 11 이하 기기에서 테스트 및 크기 조정

### 주의사항

1. **브랜드 색상 변경**: `#f28090`를 프로젝트의 브랜드 색상으로 변경
2. **테마 이름 변경**: `Theme.PocketChord`를 프로젝트의 테마 이름으로 변경
3. **로고 파일 이름 확인**: `@drawable/ic_splash_logo`를 실제 로고 파일명으로 변경
4. **시스템 바 색상**: 앱의 디자인에 맞게 상태바/네비게이션바 색상 및 아이콘 명암 조정

## 문제 해결

### 스플래시 화면이 표시되지 않음

**증상**: 앱 시작 시 흰 화면만 나타남

**해결**:
1. `AndroidManifest.xml`에서 MainActivity의 `android:theme` 확인
2. `splash_background.xml` 파일 존재 여부 확인
3. Logcat에서 리소스 관련 오류 확인

### 로고 크기가 여전히 다름

**해결**:
1. `splash_background.xml`의 width/height 값 10dp 단위로 조정
2. 실제 기기에서 테스트 (에뮬레이터는 부정확할 수 있음)
3. 여러 기기에서 테스트하여 평균적인 크기 결정

### 시스템 바 색상이 적용되지 않음

**해결**:
1. `windowDrawsSystemBarBackgrounds`가 `true`인지 확인
2. Android 버전 확인 (일부 속성은 특정 버전 이상에서만 동작)
3. `windowLightStatusBar` 등은 API 23+ 에서만 동작

## 참고 자료

- [Android Developers - Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Android 12 Splash Screen API Guide](https://developer.android.com/about/versions/12/features/splash-screen)
- [Material Design - Launch screens](https://m3.material.io/styles/motion/transitions/applying-transitions)

## 변경 이력

- 2025-01-05: 초기 문서 작성 (Pixel 4a 로고 크기 수정 내용 문서화)

