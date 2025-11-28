# Play Console 경고 해결 가이드

## 📋 일반적인 경고 및 해결 방법

### 1. INTERNET 권한 경고

**경고 메시지**:
```
버전 코드 XXXXXXXXXX인 APK의 사용하는 android.permission.INTERNET 권한을 
수락해야 할 수 있습니다. 이 앱은 인터넷 사용자가 이 앱 버전으로 
업그레이드하지 않을 수도 있습니다.
```

**의미**:
- 이전 버전에는 없었던 INTERNET 권한을 새로 추가함
- 사용자가 업데이트 시 권한 동의 필요

**해결 방법**: ✅ **무시 (정상)**

**이유**:
1. **AdMob 광고 사용 시 필수 권한**
   - 광고를 로드하려면 인터넷 연결 필요
   - 대부분의 앱이 사용하는 일반 권한

2. **사용자 거부 가능성 매우 낮음**
   - 위험 권한(Dangerous Permission)이 아님
   - 설치 시 자동 승인됨
   - 사용자가 수동으로 거부할 필요 없음

3. **Play Console의 정보성 알림**
   - 앱 출시에 영향 없음
   - 개발자에게 변경 사항 알려주는 용도

**확인**:
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

---

### 2. 네이티브 디버그 심볼 누락 경고

**경고 메시지**:
```
이 App Bundle 아티팩트 유형은 네이티브 코드를 포함하며 아직 
디버그 기호가 업로드되지 않았습니다. 비정상 종료 및 ANR을 더 
쉽게 분석하고 디버그할 수 있도록 기호 파일을 업로드하는 것이 좋습니다.
```

**의미**:
- 앱에 네이티브 코드(C/C++)가 포함됨
- 크래시 발생 시 스택 트레이스 분석 어려움
- 디버그 심볼 업로드 권장

**해결 방법**: ⚠️ **권장 (선택 사항)**

#### 방법 1: 자동 생성 설정 (이미 완료됨)

**파일**: `app/build.gradle.kts`

```kotlin
defaultConfig {
    // ...
    ndk {
        debugSymbolLevel = "SYMBOL_TABLE"  // ✅ 이미 설정됨
    }
}
```

**효과**:
- 다음 빌드부터 심볼 파일 자동 생성
- `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`

#### 방법 2: 수동 업로드 (현재 버전)

**로컬에서 심볼 파일 찾기**:
```powershell
# 심볼 파일 위치
Get-ChildItem -Path "app\build\outputs\native-debug-symbols" -Recurse -Filter "*.zip"
```

**Play Console 업로드**:
1. Play Console → 앱 선택
2. **릴리즈** → **App Bundle Explorer**
3. 버전 선택 → **다운로드** 탭
4. **네이티브 디버그 기호** 업로드

**또는**:
1. **앱 품질** → **Android Vitals**
2. **네이티브 디버그 기호** 섹션
3. 심볼 파일 업로드

**필요성**:
- ✅ **권장**: 크래시 분석 쉬워짐
- ⚠️ **선택**: 앱 출시에 필수 아님
- 📊 **효과**: 크래시 리포트가 읽기 쉬운 형태로 표시

---

### 3. ProGuard 매핑 파일 업로드

**경고 메시지**:
```
이 App Bundle은 난독화되었습니다. ProGuard 매핑 파일을 업로드하면 
크래시 및 ANR을 더 쉽게 디버그할 수 있습니다.
```

**해결 방법**: ✅ **필수 권장**

**매핑 파일 위치**:
```
app\build\outputs\mapping\release\mapping.txt
```

**업로드 방법**:
1. Play Console → 앱 선택
2. **릴리즈** → **App Bundle Explorer**
3. 버전 선택 → **다운로드** 탭
4. **ProGuard 매핑 파일** 업로드

**또는**:
```kotlin
// build.gradle.kts에 자동 업로드 설정 (Play Console API 필요)
// 수동 업로드가 더 간단함
```

---

### 4. SDK 버전 경고

**경고 메시지**:
```
이 릴리스는 최신 Android 버전을 타겟팅하지 않습니다.
```

**해결 방법**: ✅ **필수**

```kotlin
// build.gradle.kts
android {
    compileSdk = 36        // 최신 버전 사용 ✅
    defaultConfig {
        targetSdk = 36     // 최신 버전 사용 ✅
    }
}
```

**중요성**: 
- Google Play 정책 준수
- 최신 API 사용 가능
- 보안 업데이트 적용

---

### 5. 앱 서명 관련 경고

**경고 메시지**:
```
Google Play 앱 서명을 사용하지 않습니다.
```

**해결 방법**: ✅ **권장**

1. Play Console → 앱 → **릴리즈** → **설정**
2. **App Integrity** 진입
3. **Google Play 앱 서명** 활성화

**장점**:
- 업로드 키 분실 시 복구 가능
- Google이 서명 키 안전 보관
- 보안 강화

---

## 🎯 경고 우선순위

### 🔴 필수 (즉시 조치)
- SDK 버전 미달 (targetSdk 최신 버전 아님)
- ProGuard 매핑 파일 누락 (Release 빌드)

### 🟡 권장 (가능한 조치)
- 네이티브 디버그 심볼 누락
- Google Play 앱 서명 미사용

### 🟢 정보성 (무시 가능)
- INTERNET 권한 추가 알림
- 기타 권한 변경 알림

---

## 📝 체크리스트

**릴리즈 전 확인**:
- [ ] targetSdk = 최신 버전 (36)
- [ ] ProGuard 매핑 파일 준비 (`mapping.txt`)
- [ ] 네이티브 심볼 설정 (`debugSymbolLevel = "SYMBOL_TABLE"`)
- [ ] Google Play 앱 서명 활성화

**릴리즈 후 확인**:
- [ ] ProGuard 매핑 파일 업로드
- [ ] 네이티브 디버그 심볼 업로드 (있는 경우)
- [ ] 경고 메시지 검토

---

## 🚀 자동화 (선택)

**향후 개선**:
```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            // 매핑 파일 보관
            postprocessing {
                isRemoveUnusedCode = true
                isObfuscate = true
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}
```

**Play Console API 연동** (고급):
- 매핑 파일 자동 업로드
- 심볼 파일 자동 업로드
- CI/CD 파이프라인 통합

---

## 📚 참고 문서

- [Play Console 경고 해결](https://support.google.com/googleplay/android-developer/answer/9848633)
- [앱 서명](https://support.google.com/googleplay/android-developer/answer/9842756)
- [ProGuard 매핑](https://developer.android.com/studio/build/shrink-code#decode-stack-trace)
- [네이티브 디버그 심볼](https://developer.android.com/ndk/guides/ndk-build)

---

**문서 끝** - Play Console 경고 대응 완료 ✅

