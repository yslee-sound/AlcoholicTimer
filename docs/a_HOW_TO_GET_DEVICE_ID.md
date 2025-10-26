# 📱 본인 기기 ID 확인하는 방법

**목적**: AdMob 테스트 기기 등록을 위한 기기 ID 확인

---

## ⚡ 빠른 선택 가이드

```
USB 연결 가능? → 방법 1 (adb 명령어) - 가장 빠름! ⭐
앱 실행 가능? → 방법 2 (Logcat) - 가장 정확함!
코드 추가 가능? → 방법 3 (앱 내부 표시) - 가장 편함!
```

---

## 방법 1: adb 명령어로 확인 (가장 빠름!) ⭐

### 준비물
- USB 케이블
- 개발자 옵션 활성화 및 USB 디버깅 ON

### 단계

1. **개발자 옵션 활성화** (아직 안 했다면)
   ```
   설정 > 휴대전화 정보 > 빌드 번호를 7번 탭
   ```

2. **USB 디버깅 활성화**
   ```
   설정 > 개발자 옵션 > USB 디버깅 ON
   ```

3. **USB로 폰 연결**

4. **명령어 실행**
   ```bash
   # 기기 연결 확인
   adb devices
   
   # 기기 ID 확인
   adb shell settings get secure android_id
   ```

5. **결과 예시**
   ```
   a1b2c3d4e5f6g7h8
   ```

### ✅ 이 ID를 AdMob 테스트 기기로 등록하세요!

---

## 방법 2: Logcat에서 확인 (가장 정확함!)

### 단계

1. **앱 설치 및 실행**
   ```bash
   gradlew installDebug
   ```

2. **Android Studio > Logcat 열기**
   - 하단 탭에서 "Logcat" 클릭

3. **앱 실행** (폰에서)

4. **Logcat 검색**
   - 필터에 `Ads` 입력

5. **다음과 같은 로그 찾기**
   ```
   I/Ads: Use RequestConfiguration.Builder()
         .setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
         to get test ads on this device.
   ```

6. **기기 ID 복사**
   - 위 예시: `33BE2250B43518CCDA7DE426D04EE231`

### ✅ 이 ID가 AdMob 테스트 기기 ID입니다!

---

## 방법 3: 앱 내부에 표시 (가장 편함!)

### 코드 추가

**파일**: `app/src/main/java/com/sweetapps/alcoholictimer/MainApplication.kt`

```kotlin
override fun onCreate() {
    super.onCreate()
    
    // 기기 ID를 로그에 출력
    val androidId = android.provider.Settings.Secure.getString(
        contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )
    android.util.Log.d("DeviceID", "내 기기 ID: $androidId")
    
    // ... 기존 코드
}
```

### 확인

1. **앱 실행**
2. **Logcat에서 "DeviceID" 검색**
3. **기기 ID 확인**
   ```
   D/DeviceID: 내 기기 ID: a1b2c3d4e5f6g7h8
   ```

---

## 방법 4: 앱 화면에 직접 표시

### 임시 코드 추가

**파일**: `app/src/main/java/com/sweetapps/alcoholictimer/feature/start/StartActivity.kt`

StartScreen에 임시로 Text 추가:

```kotlin
@Composable
fun StartScreen(...) {
    val context = LocalContext.current
    val androidId = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    
    Column {
        // 임시: 기기 ID 표시 (확인 후 삭제)
        Text(
            text = "기기 ID: $androidId",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
        
        // ... 기존 UI
    }
}
```

### 확인 후 삭제

1. 앱 실행
2. 화면에 표시된 ID 복사
3. **반드시 이 코드는 삭제하세요!** (사용자에게 노출되면 안 됨)

---

## 📝 기기 ID를 얻었다면?

### 다음 단계

1. **MainApplication.kt 수정**
   ```kotlin
   val testDeviceIds = listOf(
       "33BE2250B43518CCDA7DE426D04EE231",  // 기존
       "YOUR_DEVICE_ID_HERE"  // ← 확인한 ID로 교체
   )
   ```

2. **앱 재빌드**
   ```bash
   gradlew clean
   gradlew installDebug
   ```

3. **확인**
   - 앱 실행 → 광고 영역에 "Test Ad" 라벨 표시 ✅

---

## 🔍 문제 해결

### adb 명령어가 안 돼요
**원인**: adb가 PATH에 없거나 기기가 연결 안 됨

**해결**:
1. Android Studio SDK Manager에서 Platform Tools 설치 확인
2. 환경 변수 PATH에 adb 경로 추가
   ```
   C:\Users\[사용자명]\AppData\Local\Android\Sdk\platform-tools
   ```
3. 기기 USB 디버깅 확인
4. USB 케이블 교체 시도

### Logcat에 로그가 안 나와요
**원인**: 광고가 로드되지 않았거나 필터가 잘못됨

**해결**:
1. 앱을 완전히 종료 후 재실행
2. Logcat 필터 제거 (All 선택)
3. "testDeviceIds" 또는 "Ads" 검색
4. 광고가 표시되는 화면으로 이동

### android_id가 null이에요
**원인**: 권한 문제 또는 에뮬레이터

**해결**:
1. 실제 기기 사용 (에뮬레이터는 ID가 다를 수 있음)
2. Logcat 방법 사용 (방법 2)

---

## 💡 팁

### 여러 기기가 있다면?
```kotlin
val testDeviceIds = listOf(
    "ABC123",  // 내 폰 1
    "DEF456",  // 내 폰 2
    "GHI789"   // 테스트 기기
)
```

### 에뮬레이터는?
- 에뮬레이터는 자동으로 테스트 기기로 인식됩니다!
- 별도 등록 불필요 ✅

### 기기 ID 관리
```
📱 갤럭시 S21: ABC123DEF456
📱 픽셀 6: XYZ789GHI012
🖥️ 에뮬레이터: 자동 인식
```

파일로 저장해두면 나중에 편함!

---

## 🎯 요약

| 방법 | 속도 | 난이도 | 정확도 |
|------|------|--------|--------|
| adb 명령어 | ⭐⭐⭐ | 쉬움 | 높음 |
| Logcat | ⭐⭐ | 중간 | 매우 높음 |
| 앱 내부 로그 | ⭐⭐ | 쉬움 | 높음 |
| 화면 표시 | ⭐ | 쉬움 | 높음 |

**추천**: adb 명령어 (가장 빠르고 간단!)

---

**마지막 업데이트**: 2025-10-26  
**난이도**: ⭐ 매우 쉬움  
**소요 시간**: 2분

