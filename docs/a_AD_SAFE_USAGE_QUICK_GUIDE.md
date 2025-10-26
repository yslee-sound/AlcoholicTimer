# 🛡️ 본인 앱 안전하게 사용하기 - 빠른 가이드

**목적**: 본인이 만든 앱을 실수로 광고 클릭 없이 안전하게 사용하는 방법

---

## ⚡ 30초 요약

```
1. 앱 실행 → Logcat에서 본인 기기 ID 확인
2. MainApplication.kt에 기기 ID 추가
3. 앱 재설치
4. ✅ 이제 실수로 클릭해도 안전!
```

---

## 📋 단계별 가이드

### 1️⃣ 본인 기기 ID 확인

#### 방법 A: Android Studio Logcat 사용 (권장)

1. **앱 설치 및 실행**
   ```bash
   gradlew installDebug
   # 또는 릴리즈 빌드
   gradlew installRelease
   ```

2. **Android Studio > Logcat 열기**
   - 하단 탭에서 "Logcat" 클릭

3. **"Ads" 또는 "testDeviceIds" 검색**
   - 필터에 `Ads` 입력

4. **다음과 같은 로그 찾기**:
   ```
   I/Ads: Use RequestConfiguration.Builder()
         .setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
         to get test ads on this device.
   ```

5. **기기 ID 복사**
   - 위 예시: `33BE2250B43518CCDA7DE426D04EE231`

#### 방법 B: adb 명령어 사용

```bash
# USB로 기기 연결 후
adb shell settings get secure android_id
```

---

### 2️⃣ MainApplication.kt에 기기 ID 추가

**파일 위치**: `app/src/main/java/com/sweetapps/alcoholictimer/MainApplication.kt`

#### 옵션 1: 디버그 빌드에만 적용 (개발 중)

```kotlin
val testDeviceIds = if (BuildConfig.DEBUG) {
    listOf(
        "33BE2250B43518CCDA7DE426D04EE231",  // 샘플
        "YOUR_DEVICE_ID_HERE"  // ← 본인 기기 ID로 교체
    )
} else {
    emptyList()
}
```

#### 옵션 2: 릴리즈 빌드에도 적용 (출시 후에도 사용)

```kotlin
val testDeviceIds = if (BuildConfig.DEBUG) {
    listOf(
        "33BE2250B43518CCDA7DE426D04EE231",
        "YOUR_DEVICE_ID_HERE"  // 본인 기기
    )
} else {
    // 릴리즈에서도 본인 기기는 테스트 광고만 표시
    listOf(
        "YOUR_DEVICE_ID_HERE"  // 본인 기기 (실수 클릭 방지)
    )
}
```

#### 옵션 3: 항상 적용 (가장 안전!)

```kotlin
// BuildConfig 조건 제거하고 항상 적용
val testDeviceIds = listOf(
    "33BE2250B43518CCDA7DE426D04EE231",  // 샘플
    "YOUR_DEVICE_ID_HERE"  // 본인 기기 (영구 등록)
)
```

**💡 추천**: 옵션 3 (항상 적용)
- 가장 안전
- 실수 클릭 완전 방지
- 앱을 자유롭게 사용 가능

---

### 3️⃣ 앱 재빌드 및 설치

```bash
# 디버그 빌드
gradlew installDebug

# 또는 릴리즈 빌드
gradlew installRelease
```

---

### 4️⃣ 확인

앱 실행 후 광고 영역을 확인하세요:

#### ✅ 성공 (테스트 광고 표시)
```
광고 상단에 "Test Ad" 또는 "Google Test Ad" 라벨 표시
→ 이제 클릭해도 안전합니다!
```

#### ❌ 실패 (실제 광고 표시)
```
광고에 "Test Ad" 라벨 없음
→ 기기 ID를 다시 확인하세요
→ 앱을 재빌드/재설치 했는지 확인
```

---

## 🎯 실전 예시

### 예시 1: 개발자의 갤럭시 S21

1. **기기 ID 확인**
   ```
   Logcat: ABC123DEF456789
   ```

2. **MainApplication.kt 수정**
   ```kotlin
   val testDeviceIds = listOf(
       "ABC123DEF456789"  // 내 갤럭시 S21
   )
   ```

3. **앱 재설치**
   ```bash
   gradlew installRelease
   ```

4. **결과**
   - ✅ 테스트 광고만 표시
   - ✅ 실수로 클릭해도 안전
   - ✅ 다른 사용자는 정상적으로 실제 광고 표시

### 예시 2: 여러 기기 등록 (개발자 + 테스터)

```kotlin
val testDeviceIds = listOf(
    "ABC123DEF456789",  // 내 갤럭시 S21
    "XYZ789ABC123456",  // 내 픽셀 6
    "QWE456RTY789012"   // 테스터 기기
)
```

---

## ⚠️ 중요 사항

### ✅ 이렇게 하세요

```
✓ 본인 기기를 테스트 기기로 등록
✓ 앱 재빌드/재설치 후 "Test Ad" 확인
✓ 테스트 광고는 마음껏 클릭해도 안전
```

### ❌ 하지 마세요

```
✗ 테스트 기기 등록 없이 실제 광고 클릭
✗ "한 번만" 클릭해보기
✗ VPN 켜고 클릭하기
```

---

## 🔍 문제 해결

### Q: "Test Ad" 라벨이 안 보여요
**A**: 
1. 기기 ID가 정확한지 확인
2. 앱을 재빌드했는지 확인
3. 앱을 완전히 삭제 후 재설치
4. Logcat에서 "test device" 메시지 확인

### Q: 실제 광고를 보고 싶어요
**A**: 
1. 다른 테스트 기기 사용
2. 또는 지인에게 스크린샷 요청
3. Play Console 스크린샷으로 확인

### Q: 실수로 클릭했어요!
**A**: 
1. 즉시 클릭 중단
2. 본인 기기를 테스트 기기로 등록
3. 앱 재설치
4. 24시간 AdMob 콘솔 모니터링
5. 상세 가이드: `docs/AD_SELF_CLICK_WARNING.md`

---

## 📚 추가 자료

- **상세 가이드**: `docs/AD_SELF_CLICK_WARNING.md`
- **정책 검토**: `docs/AD_POLICY_COMPLIANCE_SUMMARY.md`
- **전체 리뷰**: `docs/AD_POLICY_COMPLIANCE_REVIEW.md`

---

## ✅ 체크리스트

출시 전:
- [ ] 본인 기기 ID 확인
- [ ] MainApplication.kt에 기기 ID 추가
- [ ] 앱 재설치 후 "Test Ad" 확인
- [ ] 테스트 광고 클릭해서 동작 확인

출시 후:
- [ ] 본인 기기에서 테스트 광고만 표시되는지 확인
- [ ] 다른 사용자는 실제 광고 보는지 확인 (테스터에게 확인)
- [ ] AdMob 콘솔 정기 모니터링

---

**최종 업데이트**: 2025-10-26  
**난이도**: ⭐ 쉬움  
**소요 시간**: 5분

