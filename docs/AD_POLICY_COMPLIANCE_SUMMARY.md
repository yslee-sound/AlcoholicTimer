# 구글 애드몹 정책 검토 요약

**날짜**: 2025-10-26  
**앱**: AlcoholicTimer  
**결론**: ✅ **정책 준수 양호** (릴리즈 전 광고 유닛 ID 설정 필수)

---

## 📊 빠른 요약

### 정책 준수 상태
| 항목 | 상태 | 비고 |
|------|------|------|
| 광고 배치 정책 | ✅ 준수 | 명확한 구분, 고정 위치, 레이아웃 안정성 |
| 방해 광고 방지 | ✅ 준수 | 앱 시작 직후/뒤로가기 시 광고 없음 |
| 빈도 제한 | ✅ 준수 | 일일 캡 및 쿨다운 적용 |
| 사용자 동의 (UMP) | ✅ 준수 | 동의 전 광고 로드 금지 |
| 광고 콘텐츠 등급 | ✅ 준수 | Teen (T) 등급 설정 |
| 릴리즈 유닛 ID | ⚠️ **설정 필요** | 플레이스홀더 상태 |

---

## ⚠️ 필수 조치 사항

### 릴리즈 전 반드시 수정해야 할 항목

#### 1. 광고 유닛 ID 설정 (필수)
**파일**: `app/build.gradle.kts`

**현재 상태**: 플레이스홀더 문자열 사용 중
```kotlin
buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"REPLACE_WITH_REAL_BANNER\"")
```

**조치 방법**:
1. AdMob 콘솔에서 각 광고 유형별 유닛 ID 생성:
   - 배너 광고 (Anchored Adaptive Banner)
   - 전면 광고 (Interstitial)
   - 앱 오프닝 광고 (App Open)

2. `build.gradle.kts` 수정:
```kotlin
release {
    // 실제 광고 유닛 ID로 교체
    buildConfigField("String", "ADMOB_BANNER_UNIT_ID", 
        "\"ca-app-pub-8420908105703273/XXXXXXXXXX\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", 
        "\"ca-app-pub-8420908105703273/XXXXXXXXXX\"")
    buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", 
        "\"ca-app-pub-8420908105703273/XXXXXXXXXX\"")
}
```

3. 내부 테스트 트랙에서 실제 광고 표시 확인

---

## 🔧 권장 개선 사항 (선택)

### 1. 네이티브 광고 코드 정리
**이유**: 폐기된 기능이지만 코드와 리소스가 남아있음

**대상 파일**:
- `core/ads/NativeAdManager.kt`
- `core/ui/NativeExitPopup.kt`
- `core/ads/NativeViewBinder.kt`
- `layout/include_native_exit_ad.xml`

**조치 옵션**:
- **옵션 A**: 파일 삭제 (권장)
- **옵션 B**: `@Deprecated` 어노테이션 추가 후 주석 처리

**효과**:
- 앱 크기 감소
- 불필요한 네트워크 요청 제거
- 코드 유지보수성 향상

### 2. 광고 로딩 재시도 로직 추가
**대상**: `core/ui/AdBanner.kt`

**현재**: 로딩 실패 시 로그만 기록
```kotlin
override fun onAdFailedToLoad(error: LoadAdError) {
    Log.w(TAG, "Banner failed to load: ${error.message}")
}
```

**개선안**: 일정 시간 후 자동 재시도
```kotlin
override fun onAdFailedToLoad(error: LoadAdError) {
    Log.w(TAG, "Banner failed to load: ${error.message}")
    if (error.code == 2 || error.code == 0) { // 네트워크/내부 오류
        Handler(Looper.getMainLooper()).postDelayed({
            loadAd(AdRequest.Builder().build())
        }, 30000) // 30초 후 재시도
    }
}
```

---

## ✅ 정책 준수 세부 내용

### 1. 배너 광고
- ✅ 화면 하단 고정 배치
- ✅ 상단 헤어라인으로 콘텐츠와 구분
- ✅ 예측 높이로 레이아웃 점프 방지
- ✅ 시스템 UI와 겹치지 않음

### 2. 전면 광고
- ✅ 자연스러운 화면 전환 지점에만 표시
- ✅ 일일 최대 3회 제한
- ✅ 2분 쿨다운
- ✅ 앱 시작 직후 표시 금지 (콜드 스타트 게이트)

### 3. 앱 오프닝 광고
- ✅ 백그라운드→포그라운드 전환 시에만 표시
- ✅ 일일 최대 5회 제한
- ✅ 5분 쿨다운
- ✅ 콜드 스타트 시 표시 금지

### 4. ~~네이티브 광고~~ (폐기됨)
- ✅ 뒤로가기/종료 플로우에서 제거
- ✅ 정책 리스크 회피

---

## 📋 릴리즈 전 체크리스트

### 필수 항목
- [ ] **릴리즈 빌드에 실제 광고 유닛 ID 설정**
- [ ] AdMob 콘솔에서 앱 및 광고 유닛 생성 완료
- [ ] 내부 테스트 트랙에서 실제 광고 표시 확인
- [ ] UMP 동의 플로우 정상 작동 확인
- [ ] 앱 시작 직후 전면광고 표시되지 않는지 확인
- [ ] 백 버튼 눌렀을 때 광고 표시되지 않는지 확인

### 권장 항목
- [ ] 네이티브 광고 코드 정리 (선택)
- [ ] 배너 광고 재시도 로직 추가 (선택)
- [ ] 다양한 기기/화면 크기에서 테스트
- [ ] 광고 표시 빈도 모니터링 설정

---

## 🎯 최종 결론

### 정책 위반 위험도: 🟢 **낮음**
현재 구현은 AdMob 정책을 잘 준수하고 있습니다. 주요 정책 항목들을 모두 충족하며, 사용자 경험을 해치지 않는 수준에서 광고를 배치하고 있습니다.

### 수익화 준비도: 🟡 **중간**
**릴리즈 빌드에 실제 광고 유닛 ID만 설정하면** 즉시 수익화 가능합니다. 현재는 테스트 ID만 사용하여 실제 수익이 발생하지 않습니다.

### 권장 조치
1. **즉시**: 릴리즈 빌드 광고 유닛 ID 설정
2. **내부 테스트**: 실제 광고 표시 및 정책 준수 검증
3. **선택적**: 네이티브 광고 코드 정리
4. **배포**: Play Store 릴리즈

---

## 📚 참고 문서
- 상세 검토 보고서: [`AD_POLICY_COMPLIANCE_REVIEW.md`](./AD_POLICY_COMPLIANCE_REVIEW.md)
- 배너 광고 가이드: [`a_ADS_BANNER_PROMPT.md`](./a_ADS_BANNER_PROMPT.md)
- 전면 광고 가이드: [`a_ADS_INTERSTITIAL_PROMPT.md`](./a_ADS_INTERSTITIAL_PROMPT.md)
- 앱 오프닝 광고 가이드: [`a_PROMPT_APP_OPEN_AD.md`](./a_PROMPT_APP_OPEN_AD.md)
- 네이티브 광고 폐기 안내: [`a_ADS_NATIVE_BACK_PROMPT.md`](./a_ADS_NATIVE_BACK_PROMPT.md)

---

**검토 완료**: 2025-10-26  
**다음 검토**: 릴리즈 빌드 전 필수  
**문서 버전**: 1.0

