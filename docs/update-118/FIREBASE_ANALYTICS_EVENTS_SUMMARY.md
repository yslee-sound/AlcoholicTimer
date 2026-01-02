# Firebase Analytics 이벤트 전송 현황 정리

**작성일**: 2026-01-02  
**버전**: 1.1.9  
**목적**: 현재 앱에서 Firebase Analytics로 전송되는 모든 이벤트 정리 및 문서화

---

## 📊 전송 중인 이벤트 목록 (14개)

### 1. **ad_revenue** (광고 수익)
- **설명**: 광고 수익이 발생했을 때 (AdMob onPaidEvent)
- **발생 위치**:
  - `AdBanner.kt` (배너 광고)
  - `InterstitialAdManager.kt` (전면 광고)
- **파라미터**:
  - `value` (Double): 수익 금액
  - `currency` (String): 통화 (USD)
  - `ad_type` (String): "banner" / "interstitial"
- **수익화 연관도**: ★★★★★ (실제 수익 추적 - 가장 중요)
- **상태**: ✅ **구현 완료**

---

### 2. **timer_start** (타이머 시작)
- **설명**: 사용자가 금주 타이머를 시작할 때 발생
- **발생 위치**: `StartScreenViewModel.startCountdown()`
- **파라미터**:
  - `target_days` (Int): 목표 일수
  - `had_active_goal` (Boolean): 이전에 활성 목표가 있었는지 여부
  - `start_ts` (Long): 시작 타임스탬프
- **수익화 연관도**: ★★★★☆ (신규 사용자 전환 추적)

---

### 3. **timer_give_up** (타이머 포기)
- **설명**: 사용자가 타이머를 중도 포기할 때 발생
- **발생 위치**: `Tab01ViewModel.giveUpTimer()`
- **파라미터**:
  - `target_days` (Int): 목표 일수
  - `actual_days` (Int): 실제 진행한 일수
  - `quit_reason` (String): 포기 사유
  - `start_ts` (Long): 시작 타임스탬프
  - `quit_ts` (Long): 포기 타임스탬프
  - `progress_percent` (Float): 진행률 (%)
- **수익화 연관도**: ★★★★★ (이탈 사용자 분석 + 리텐션 알림 타겟팅)

---

### 4. **session_start** (세션 시작)
- **설명**: 앱 실행 시 (초기화 완료 후) 발생
- **발생 위치**: `MainActivity.sendSessionStartEvent()`
- **파라미터**:
  - `is_first_session` (Boolean): 첫 세션 여부
  - `days_since_install` (Int): 설치 후 경과 일수
  - `timer_status` (String): 현재 타이머 상태 ("active" / "inactive")
- **수익화 연관도**: ★★★★☆ (DAU/MAU, 리텐션 분석)

---

### 5. **ad_impression** (광고 노출)
- **설명**: 광고가 사용자에게 노출될 때 발생
- **발생 위치**:
  - `AdBanner.kt` (배너 광고)
  - `AppOpenAdManager.kt` (앱 오프닝 광고)
  - `InterstitialAdManager.kt` (전면 광고)
- **파라미터**:
  - `ad_type` (String): "banner" / "app_open" / "interstitial"
- **수익화 연관도**: ★★★★★ (광고 노출 추적 - 수익 핵심 지표)
- **상태**: ✅ **구현 완료**

---

### 6. **ad_click** (광고 클릭)
- **설명**: 사용자가 광고를 클릭할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `ad_type` (String): 광고 유형
- **수익화 연관도**: ★★★★★ (CTR 분석 - 수익 극대화)
- **상태**: ⚠️ **구현 필요** (AdMob SDK 콜백에서 호출)

---

### 7. **settings_change** (설정 변경)
- **설명**: 사용자가 앱 설정을 변경할 때 발생
- **발생 위치**:
  - `MainActivity.kt` (알림 권한)
  - `SettingsScreen.kt` (응원 알림 토글)
  - `CurrencyScreen.kt` (통화 설정)
- **파라미터**:
  - `setting_type` (String): "notification_permission" / "retention_notification" / "currency"
  - `old_value` (String, nullable): 이전 값
  - `new_value` (String): 새 값
- **수익화 연관도**: ★★★☆☆ (사용자 행동 패턴 분석)
- **상태**: ✅ **구현 완료**

---

### 8. **notification_open** (알림 클릭)
- **설명**: 사용자가 푸시 알림을 클릭하여 앱에 진입할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `notification_id` (Int): 알림 ID
  - `group_type` (String): "group_a" / "group_b" / "group_c"
  - `target_screen` (String): 목적지 화면
  - `open_ts` (Long): 클릭 타임스탬프
- **수익화 연관도**: ★★★★★ (리텐션 캠페인 효과 측정)
- **상태**: ⚠️ **구현 필요** (MainActivity.onNewIntent에서 호출)

---

### 9. **level_up** (레벨 업)
- **설명**: 사용자가 새로운 레벨에 도달할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `old_level` (Int): 이전 레벨
  - `new_level` (Int): 새 레벨
  - `total_days` (Int): 총 금주 일수
  - `level_name` (String): 레벨 이름
  - `achievement_ts` (Long): 달성 타임스탬프
- **수익화 연관도**: ★★★★☆ (사용자 성취도 추적 - 광고 타이밍 최적화)
- **상태**: ⚠️ **구현 필요** (UserStatusManager에서 호출)

---

### 10. **screen_view** (화면 전환)
- **설명**: 사용자가 화면을 전환할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `screen_name` (String): 화면 이름
  - `screen_class` (String): 화면 클래스명
  - `previous_screen` (String, nullable): 이전 화면
  - `timer_status` (String): 타이머 상태
- **수익화 연관도**: ★★★☆☆ (사용자 플로우 분석)
- **상태**: ⚠️ **구현 필요** (각 Composable Screen에서 호출)

---

### 11. **community_post** (커뮤니티 글 작성)
- **설명**: 사용자가 커뮤니티에 글을 작성할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `post_type` (String): "challenge" / "diary"
  - `has_image` (Boolean): 이미지 포함 여부
  - `content_length` (Int): 글자 수
  - `tag_type` (String, nullable): 태그 유형
  - `user_level` (Int): 사용자 레벨
  - `days` (Int): 현재 금주 일수
- **수익화 연관도**: ★★★★☆ (커뮤니티 활성도 추적 - 리텐션)
- **상태**: ⚠️ **구현 필요** (CommunityViewModel.addPost에서 호출)

---

### 12. **view_records** (기록 조회)
- **설명**: 사용자가 과거 금주 기록을 조회할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **수익화 연관도**: ★★☆☆☆
- **상태**: ⚠️ **구현 필요**

---

### 13. **change_record_view** (기록 필터 변경)
- **설명**: 기록 화면에서 주/월/년 필터를 변경할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `view_type` (String): "week" / "month" / "year"
  - `current_level` (Int): 현재 레벨
- **수익화 연관도**: ★★☆☆☆
- **상태**: ⚠️ **구현 필요**

---

### 14. **view_record_detail** (기록 상세 조회)
- **설명**: 특정 기록의 상세 정보를 조회할 때 발생
- **발생 위치**: (현재 미구현 - 권장 추가)
- **파라미터**:
  - `record_id` (String): 기록 ID
- **수익화 연관도**: ★★☆☆☆
- **상태**: ⚠️ **구현 필요**

---

## 🚀 수익화 측면에서 반드시 추가해야 할 이벤트

### Priority 1: 즉시 구현 필수 (High Impact)

#### 1. **ad_click** 이벤트 추가
```kotlin
// AdBanner.kt, AppOpenAdManager.kt, InterstitialAdManager.kt
adView.setOnPaidEventListener { adValue ->
    AnalyticsManager.logAdClick(adType) // ← 추가
    AnalyticsManager.logAdRevenue(...)
}
```
**이유**: 광고 클릭률(CTR) 분석으로 광고 배치 최적화 가능

---

#### 2. **notification_open** 이벤트 추가
```kotlin
// MainActivity.kt - handleDeepLinkNavigation()
intent.getStringExtra("screen_route")?.let { route ->
    val notificationId = intent.getIntExtra("notification_id", -1)
    val groupType = intent.getStringExtra("group_type", "unknown")
    
    AnalyticsManager.logNotificationOpen(
        notificationId = notificationId,
        groupType = groupType,
        targetScreen = route
    )
}
```
**이유**: 리텐션 알림 캠페인 효과 측정 (D-1, D-7 리텐션 개선)

---

#### 3. **level_up** 이벤트 추가
```kotlin
// UserStatusManager.kt - updateStatus()
if (newLevelIndex > oldLevelIndex) {
    AnalyticsManager.logLevelUp(
        oldLevel = oldLevelIndex,
        newLevel = newLevelIndex,
        totalDays = days,
        levelName = context.getString(newLevel.nameResId),
        achievementTs = System.currentTimeMillis()
    )
}
```
**이유**: 사용자 성취 지점에서 전면 광고 노출 최적화 (높은 CTR 예상)

---

### Priority 2: 중요도 높음 (Medium Impact)

#### 4. **screen_view** 이벤트 추가
```kotlin
// 각 Screen Composable
LaunchedEffect(Unit) {
    AnalyticsManager.logScreenView(
        screenName = "Records",
        screenClass = "RecordsScreen",
        previousScreen = navController.previousBackStackEntry?.destination?.route,
        timerStatus = if (startTime > 0) "active" else "inactive"
    )
}
```
**이유**: 사용자 플로우 분석 → 광고 노출 타이밍 개선

---

#### 5. **community_post** 이벤트 추가
```kotlin
// CommunityViewModel.kt - addPost()
AnalyticsManager.logCommunityPost(
    postType = if (isChallenge) "challenge" else "diary",
    hasImage = imageUrl != null,
    contentLength = content.length,
    tagType = selectedTag,
    userLevel = currentLevel,
    days = currentDays
)
```
**이유**: 커뮤니티 활성 사용자 세그먼트 분석 → 리텐션 개선

---

## 📈 User Property 설정 현황

### 현재 설정 중인 User Properties:
```kotlin
// MainActivity.sendSessionStartEvent()
AnalyticsManager.setUserProperty(
    "retention_group",
    if (retryCount == 0) "group_new_user"
    else if (isTimerActive) "group_active_user"
    else "group_resting_user"
)
```

### 추가 권장 User Properties:
1. **current_level** (String): 현재 사용자 레벨
2. **total_days** (String): 총 금주 일수 (10일 단위 구간: "0-10", "11-30", "31-100", "100+")
3. **currency** (String): 사용자 설정 통화 ("KRW", "IDR", "USD", ...)
4. **install_date** (String): 설치 날짜 (YYYY-MM-DD)
5. **ad_revenue_ltv** (String): 광고 누적 수익 구간 ("0-1", "1-5", "5-10", "10+")

---

## 🎯 Firebase 대시보드 활용 가이드

### 1. 리텐션 분석
```
이벤트: session_start
필터: 
  - days_since_install = 1 (D-1 리텐션)
  - days_since_install = 7 (D-7 리텐션)
  - days_since_install = 30 (D-30 리텐션)
```

### 2. 광고 수익 분석
```
이벤트: ad_impression
그룹화: ad_type
비교: ad_click (CTR 계산)
```

### 3. 이탈 사용자 분석
```
이벤트: timer_give_up
필터:
  - quit_reason 별 그룹화
  - progress_percent < 30 (조기 이탈 사용자)
타겟팅: 리텐션 알림 강화 대상
```

### 4. 전환 퍼널 분석
```
Step 1: session_start (is_first_session=true)
Step 2: timer_start
Step 3: level_up (new_level=2)
Step 4: ad_impression (누적 3회 이상)
```

---

## 📋 구현 체크리스트

- [x] `ad_revenue` - 구현 완료 ✅
- [x] `timer_start` - 구현 완료 ✅
- [x] `timer_give_up` - 구현 완료 ✅
- [x] `session_start` - 구현 완료 ✅
- [x] `ad_impression` - 구현 완료 ✅
- [x] `settings_change` - 구현 완료 ✅
- [ ] `ad_click` - **미구현 (Priority 1)** ⚠️
- [ ] `notification_open` - **미구현 (Priority 1)** ⚠️
- [ ] `level_up` - **미구현 (Priority 1)** ⚠️
- [ ] `screen_view` - **미구현 (Priority 2)** ⚠️
- [ ] `community_post` - **미구현 (Priority 2)** ⚠️
- [ ] `view_records` - 미구현
- [ ] `change_record_view` - 미구현
- [ ] `view_record_detail` - 미구현

**구현 완료율**: 6/14 (42.9%)

---

## 🔍 다음 단계

1. **Priority 1 이벤트 구현** (예상 소요: 2-3시간)
   - `ad_click`: 광고 관련 파일 3곳 수정
   - `notification_open`: MainActivity 딥링크 처리 로직 수정
   - `level_up`: UserStatusManager에 레벨 변경 감지 로직 추가

2. **Firebase 대시보드 커스텀 리포트 생성**
   - 광고 수익 대시보드
   - 리텐션 분석 대시보드
   - 이탈 사용자 세그먼트

3. **A/B 테스트 설계**
   - 알림 문구 테스트 (그룹 A vs 그룹 B)
   - 광고 배치 테스트 (레벨업 직후 vs 화면 전환 시)

---

**작성자**: GitHub Copilot  
**검토 필요 사항**: Priority 1 이벤트 구현 우선순위 검토

