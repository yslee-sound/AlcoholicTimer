# 📊 Firebase Analytics 이벤트 완전 가이드 (AlcoholicTimer)

**작성일**: 2025-12-31  
**버전**: v1.0  
**목적**: 앱에서 전송되는 모든 Firebase Analytics 이벤트 정리 및 수익화 관점의 권장 추가 이벤트 제안

---

## 📑 목차

1. [현재 구현된 이벤트 (9개)](#1-현재-구현된-이벤트-9개)
2. [이벤트 전송 위치 및 타이밍](#2-이벤트-전송-위치-및-타이밍)
3. [이벤트별 상세 스펙](#3-이벤트별-상세-스펙)
4. [수익화 관점 분석](#4-수익화-관점-분석)
5. [권장 추가 이벤트 (필수 7개)](#5-권장-추가-이벤트-필수-7개)
6. [구현 우선순위](#6-구현-우선순위)
7. [측정 가능한 핵심 지표](#7-측정-가능한-핵심-지표)

---

## 1. 현재 구현된 이벤트 (9개)

### ✅ 구현 완료 (8개)

| 이벤트명 | 카테고리 | 구현 위치 | 상태 |
|---------|---------|----------|------|
| `timer_start` | 타이머 | StartScreenViewModel.kt | ✅ 사용 중 |
| `timer_finish` | 타이머 | Tab01ViewModel.kt | ✅ 사용 중 |
| `ad_impression` | 광고 | AdBanner.kt, InterstitialAdManager.kt, AppOpenAdManager.kt | ✅ 사용 중 |
| `ad_click` | 광고 | AdBanner.kt, InterstitialAdManager.kt | ✅ 사용 중 |
| `ad_revenue` | 광고 수익 | AdBanner.kt, InterstitialAdManager.kt (PaidEventListener) | ✅ 사용 중 |
| `view_records` | 사용자 행동 | BottomNavBar.kt | ✅ 사용 중 |
| `change_record_view` | 사용자 행동 | RecordsScreen.kt | ✅ 사용 중 |
| `view_record_detail` | 사용자 행동 | Tab02ListGraph.kt, Tab02DetailGraph.kt | ✅ 사용 중 |

### ⚠️ 정의되었으나 미사용 (1개)

| 이벤트명 | 카테고리 | 정의 위치 | 상태 |
|---------|---------|----------|------|
| `timer_end` | 타이머 | AnalyticsManager.kt | ⚠️ 정의만 있음, 호출 안 됨 |

**`timer_end` 미사용 원인**:
- `giveUpTimer()` 함수에서 이벤트 전송 코드가 없음
- 타이머 중도 포기 시 분석 데이터 누락

---

## 2. 이벤트 전송 위치 및 타이밍

### 📍 타이머 생명주기 이벤트

```
[시작] 
Start 화면 → 목표 설정 → "시작" 버튼 클릭
└─> StartScreenViewModel.startCountdown()
    └─> AnalyticsManager.logTimerStart()
        ├─ target_days: Int
        ├─ had_active_goal: Boolean
        └─ start_ts: Long

[진행 중]
Run 화면 표시 (타이머 카운팅)

[완료]
목표 달성 → Success 화면 이동
└─> Tab01ViewModel (타이머 만료 감지)
    └─> AnalyticsManager.logTimerFinish()
        ├─ target_days: Int
        ├─ actual_days: Int
        ├─ start_ts: Long
        └─ end_ts: Long

[중도 포기] ⚠️ 현재 이벤트 없음!
Quit 화면 → "포기" 확인
└─> Tab01ViewModel.giveUpTimer()
    └─> [현재 Analytics 이벤트 전송 안 됨]
```

### 📍 광고 이벤트 흐름

```
[광고 노출]
광고 표시 시점
└─> AdListener.onAdImpression()
    └─> AnalyticsManager.logAdImpression(ad_type)

[광고 클릭]
사용자가 광고 클릭
└─> AdListener.onAdClicked()
    └─> AnalyticsManager.logAdClick(ad_type)

[광고 수익 발생]
광고 플랫폼에서 수익 이벤트 수신
└─> PaidEventListener
    └─> AnalyticsManager.logAdRevenue(value, currency, ad_type)
```

### 📍 사용자 행동 이벤트

```
[기록 탭 진입]
BottomNavBar 2번째 버튼 클릭
└─> AnalyticsManager.logViewRecords()

[기록 보기 형식 변경]
Records 화면에서 주/월/년 전환
└─> AnalyticsManager.logChangeRecordView(view_type, current_level)

[기록 상세 조회]
특정 기록 아이템 클릭
└─> AnalyticsManager.logViewRecordDetail(record_id)
```

---

## 3. 이벤트별 상세 스펙

### 3.1 타이머 이벤트

#### `timer_start` - 금주 타이머 시작

**파라미터**:
```kotlin
{
  "target_days": Int,         // 목표 일수 (예: 7, 30, 100)
  "had_active_goal": Boolean, // 이전 진행 중인 목표가 있었는지
  "start_ts": Long           // 시작 타임스탬프 (UTC milliseconds)
}
```

**비즈니스 의미**:
- 사용자 활성화(Activation) 지표
- 목표 설정 패턴 분석 (7일 vs 30일 선호도)
- 재시작 여부 파악 (재도전율 계산)

**예시**:
```
Event: timer_start
Parameters: { target_days=30, had_active_goal=false, start_ts=1735603200000 }
```

---

#### `timer_finish` - 금주 목표 달성

**파라미터**:
```kotlin
{
  "target_days": Int,   // 목표 일수
  "actual_days": Int,   // 실제 달성 일수
  "start_ts": Long,     // 시작 타임스탬프
  "end_ts": Long        // 완료 타임스탬프
}
```

**비즈니스 의미**:
- 목표 달성률(Completion Rate) 계산
- 성공 사용자 세그먼트 분석
- LTV 예측 (성공 사용자 = 높은 LTV)

**예시**:
```
Event: timer_finish
Parameters: { target_days=30, actual_days=30, start_ts=1735603200000, end_ts=1738281600000 }
```

---

#### `timer_end` - 타이머 중도 포기 ⚠️ **현재 미구현**

**파라미터** (정의됨):
```kotlin
{
  "target_days": Int,      // 목표 일수
  "actual_days": Int,      // 실제 진행 일수
  "fail_reason": String,   // 포기 이유
  "start_ts": Long,        // 시작 타임스탬프
  "end_ts": Long          // 종료 타임스탬프
}
```

**구현 필요성**: 🔴 **높음**
- 이탈(Churn) 분석 필수
- 어느 시점에서 포기하는지 파악 (3일차? 7일차?)
- 실패 원인 분석으로 UX 개선

---

### 3.2 광고 이벤트

#### `ad_impression` - 광고 노출

**파라미터**:
```kotlin
{
  "ad_type": String  // "banner" | "interstitial" | "app_open"
}
```

**구현 위치**:
- Banner: `AdBanner.kt` (Line 284)
- Interstitial: `InterstitialAdManager.kt` (Line 165, 291)
- AppOpen: `AppOpenAdManager.kt` (Line 443)

---

#### `ad_click` - 광고 클릭

**파라미터**:
```kotlin
{
  "ad_type": String  // "banner" | "interstitial"
}
```

**CTR 계산**: `ad_click / ad_impression`

---

#### `ad_revenue` - 광고 수익 발생

**파라미터**:
```kotlin
{
  "value": Double,     // 수익 금액 (예: 0.05)
  "currency": String,  // 통화 (예: "USD")
  "ad_type": String   // 광고 유형
}
```

**수익 분석**: Firebase와 AdMob 수익 연동

---

### 3.3 사용자 행동 이벤트

#### `view_records` - 기록 탭 진입

**파라미터**: 없음

**비즈니스 의미**: 사용자 참여도(Engagement) 측정

---

#### `change_record_view` - 기록 보기 형식 변경

**파라미터**:
```kotlin
{
  "view_type": String,     // 보기 형식
  "current_level": Int     // 현재 레벨
}
```

---

#### `view_record_detail` - 기록 상세 조회

**파라미터**:
```kotlin
{
  "record_id": String  // 기록 ID
}
```

---

## 4. 수익화 관점 분석

### 💰 현재 수익 구조

AlcoholicTimer는 **광고 기반 수익 모델**을 사용합니다:

1. **App Open Ad** (앱 실행 시)
2. **Interstitial Ad** (전면 광고, 현재 비활성화)
3. **Native Ad** (피드형 광고)
4. **배너 광고** (현재 제거됨)

### 📊 수익화에 필수적인 지표

#### 현재 측정 가능한 지표:
- ✅ **광고 노출 수** (`ad_impression`)
- ✅ **광고 클릭률(CTR)** (`ad_click / ad_impression`)
- ✅ **광고 수익** (`ad_revenue`)

#### 현재 측정 불가능한 중요 지표:
- ❌ **사용자 유지율(Retention)** - D1, D7, D30
- ❌ **이탈률(Churn Rate)** - 타이머 포기 시점
- ❌ **세션 깊이(Session Depth)** - 화면 탐색 패턴
- ❌ **기능 사용률** - 어떤 기능을 많이 쓰는지
- ❌ **사용자 가치(LTV)** - 장기 사용자 vs 단기 사용자
- ❌ **재방문 의도** - 목표 달성 후 재시작률

### 🎯 수익화 최적화 전략

```
높은 LTV 사용자 = 오래 사용하는 사용자
                = 더 많은 광고 노출
                = 더 높은 수익
```

**핵심 질문**:
1. 어떤 사용자가 오래 사용하는가? → **사용자 세그먼트 분석 필요**
2. 어느 시점에 이탈하는가? → **Churn 분석 필요**
3. 무엇이 재방문을 유도하는가? → **Retention 트리거 분석 필요**

---

## 5. 권장 추가 이벤트 (필수 7개)

### 🔴 우선순위 1: 즉시 구현 필요 (4개)

#### 5.1 `timer_give_up` - 타이머 포기 ⭐⭐⭐⭐⭐

**목적**: 이탈 분석의 핵심 지표

**파라미터**:
```kotlin
{
  "target_days": Int,        // 목표 일수
  "actual_days": Int,        // 실제 진행 일수
  "quit_reason": String,     // "user_quit" | "accidental" | "app_error"
  "start_ts": Long,          // 시작 타임스탬프
  "quit_ts": Long,           // 포기 타임스탬프
  "progress_percent": Float  // 진행률 (actual_days / target_days * 100)
}
```

**구현 위치**: `Tab01ViewModel.giveUpTimer()` (Line 304)

**분석 가능한 지표**:
- 평균 포기 시점 (3일차? 7일차?)
- 목표 난이도별 포기율 (7일 목표 vs 30일 목표)
- 진행률별 포기 분포 (50% 진행 후 포기가 많은가?)

**비즈니스 임팩트**: 🔥 **매우 높음**
- Churn 방지 전략 수립 가능
- 푸시 알림 타이밍 최적화
- 목표 설정 UX 개선

**예시**:
```kotlin
// Tab01ViewModel.giveUpTimer() 내부에 추가
AnalyticsManager.logTimerGiveUp(
    targetDays = targetDays.toInt(),
    actualDays = actualDays.toInt(),
    quitReason = "user_quit",
    startTs = startTime,
    quitTs = endTime,
    progressPercent = (actualDays / targetDays * 100).toFloat()
)
```

---

#### 5.2 `session_start` - 세션 시작 ⭐⭐⭐⭐⭐

**목적**: 사용자 활성도 및 앱 사용 빈도 측정

**파라미터**:
```kotlin
{
  "is_first_session": Boolean,  // 첫 실행 여부
  "days_since_install": Int,    // 설치 후 경과 일수
  "timer_status": String        // "active" | "idle" | "completed"
}
```

**구현 위치**: `MainActivity.onCreate()` 또는 `MainApplication.onCreate()`

**분석 가능한 지표**:
- DAU (Daily Active Users)
- Session Frequency (하루 몇 번 접속?)
- D1, D7, D30 Retention 계산

**비즈니스 임팩트**: 🔥 **매우 높음**
- UA(User Acquisition) 효율 측정
- 광고 노출 기회 예측
- Lifetime Sessions 계산

---

#### 5.3 `level_up` - 레벨 업 달성 ⭐⭐⭐⭐

**목적**: 사용자 성취 및 참여도 측정

**파라미터**:
```kotlin
{
  "old_level": Int,          // 이전 레벨 (예: 1)
  "new_level": Int,          // 새 레벨 (예: 2)
  "total_days": Int,         // 누적 일수
  "level_name": String,      // 레벨명 (예: "Novice", "Enthusiast")
  "achievement_ts": Long     // 달성 시각
}
```

**구현 위치**: `UserStatusManager` 또는 레벨 변경 감지 로직

**분석 가능한 지표**:
- 레벨별 사용자 분포 (대부분 Lv.2에서 이탈?)
- 레벨 진행 속도 (빠른 레벨업 = 높은 참여도)
- 레벨별 광고 수익 비교

**비즈니스 임팩트**: 🔥 **높음**
- 게임화(Gamification) 효과 측정
- 성취감 기반 Retention 분석
- 마일스톤 보상 최적화

**예시**:
```kotlin
// UserStatusManager 또는 레벨 변경 감지 시
if (newLevel > oldLevel) {
    AnalyticsManager.logLevelUp(
        oldLevel = oldLevel,
        newLevel = newLevel,
        totalDays = totalDays,
        levelName = LevelDefinitions.getLevelInfo(totalDays).nameResId.toString(),
        achievementTs = System.currentTimeMillis()
    )
}
```

---

#### 5.4 `screen_view` - 화면 전환 ⭐⭐⭐⭐

**목적**: 사용자 탐색 패턴 및 기능 사용률 분석

**파라미터**:
```kotlin
{
  "screen_name": String,      // 화면 이름
  "screen_class": String,     // 클래스명
  "previous_screen": String?, // 이전 화면
  "timer_status": String      // 타이머 상태
}
```

**구현 위치**: `AppNavHost` 또는 각 Composable 화면

**분석 가능한 지표**:
- 인기 화면 (어떤 기능을 많이 쓰는가?)
- 화면 이동 경로 (User Flow)
- 세션당 화면 수 (Session Depth)

**비즈니스 임팩트**: 🔥 **높음**
- 미사용 기능 발견 및 제거
- 핵심 기능 강화
- 네비게이션 UX 개선

**화면 목록** (NavRoutes.kt 기준):
```kotlin
// Tab 1 (타이머)
- "start"     // 시작 화면
- "run"       // 진행 화면
- "quit"      // 포기 확인
- "success"   // 목표 달성
- "giveup"    // 중단 결과

// Tab 2 (기록)
- "records"         // 기록 요약
- "all_records"     // 전체 기록
- "all_diaries"     // 전체 일기
- "level_detail"    // 레벨 상세
- "detail"          // 기록 상세

// Tab 3 (커뮤니티)
- "more"      // 커뮤니티 메인

// Tab 4 (설정)
- "about"              // 설정 메인
- "profile_edit"       // 프로필 편집
- "currency_settings"  // 통화 설정
- "debug"             // 디버그 화면
```

---

### 🟡 우선순위 2: 중요도 높음 (3개)

#### 5.5 `community_post` - 커뮤니티 글 작성 ⭐⭐⭐

**목적**: 커뮤니티 참여도 및 사용자 활성화 측정

**파라미터**:
```kotlin
{
  "post_type": String,      // "community" | "diary"
  "has_image": Boolean,     // 이미지 포함 여부
  "content_length": Int,    // 글자 수
  "tag_type": String?,      // "thanks" | "reflect" | "cheer"
  "user_level": Int,        // 작성자 레벨
  "days": Int              // 금주 일수
}
```

**구현 위치**: `CommunityViewModel.publishPost()` (Line 442)

**분석 가능한 지표**:
- 커뮤니티 활성도
- 레벨별 참여율 (고레벨 = 높은 참여?)
- 콘텐츠 품질 (이미지 포함 vs 텍스트만)

**비즈니스 임팩트**: 🔥 **중간**
- 소셜 기능의 가치 검증
- UGC(User Generated Content) 활성화
- 커뮤니티 기반 Retention 향상

---

#### 5.6 `settings_change` - 설정 변경 ⭐⭐⭐

**목적**: 사용자 맞춤화 패턴 분석

**파라미터**:
```kotlin
{
  "setting_type": String,    // "language" | "currency" | "habit" | "notification"
  "old_value": String?,      // 이전 값
  "new_value": String        // 새 값
}
```

**구현 위치**: 각 설정 화면

**분석 가능한 지표**:
- 언어 분포 (한국어 vs 영어 사용자 비율)
- 개인화 수준 (설정 변경 빈도)

---

#### 5.7 `share_achievement` - 성취 공유 ⭐⭐⭐

**목적**: 바이럴 효과 및 소셜 확산 측정

**파라미터**:
```kotlin
{
  "achievement_type": String,  // "goal_complete" | "level_up" | "milestone"
  "share_platform": String,    // "kakao" | "twitter" | "instagram" | "copy"
  "achievement_value": Int     // 달성 일수 또는 레벨
}
```

**구현 위치**: Success 화면 또는 LevelDetail 화면

**비즈니스 임팩트**: 🔥 **높음**
- 자연 유입(Organic Growth) 측정
- K-Factor 계산
- 입소문 마케팅 효과

---

## 6. 구현 우선순위

### Phase 1: 즉시 구현 (1주일 이내) 🔴

| 순위 | 이벤트 | 난이도 | 예상 시간 | ROI |
|-----|--------|--------|----------|-----|
| 1 | `timer_give_up` | ⭐ 쉬움 | 30분 | 🔥 매우 높음 |
| 2 | `session_start` | ⭐⭐ 보통 | 1시간 | 🔥 매우 높음 |
| 3 | `screen_view` | ⭐⭐⭐ 어려움 | 2시간 | 🔥 높음 |
| 4 | `level_up` | ⭐⭐ 보통 | 1시간 | 🔥 높음 |

**총 예상 시간**: 4.5시간  
**비즈니스 임팩트**: 수익화 최적화의 80% 달성

---

### Phase 2: 단기 구현 (1개월 이내) 🟡

| 순위 | 이벤트 | 난이도 | 예상 시간 | ROI |
|-----|--------|--------|----------|-----|
| 5 | `community_post` | ⭐ 쉬움 | 30분 | 🔥 중간 |
| 6 | `settings_change` | ⭐ 쉬움 | 30분 | 🔥 중간 |
| 7 | `share_achievement` | ⭐⭐⭐ 어려움 | 3시간 | 🔥 높음 (장기) |

---

### Phase 3: 장기 구현 (선택 사항) 🟢

추가로 고려할 이벤트:
- `diary_write` - 일기 작성
- `notification_open` - 푸시 알림 클릭
- `tutorial_complete` - 온보딩 완료
- `error_occurred` - 앱 에러 발생
- `search_query` - 검색 기능 (미래)

---

## 7. 측정 가능한 핵심 지표

### 7.1 현재 측정 가능한 지표 (구현된 이벤트 기반)

#### 광고 수익 지표:
```
✅ Ad Impressions = COUNT(ad_impression)
✅ Ad Clicks = COUNT(ad_click)
✅ CTR = ad_click / ad_impression * 100
✅ Total Revenue = SUM(ad_revenue.value)
✅ eCPM = (Total Revenue / Ad Impressions) * 1000
✅ Revenue by Ad Type = SUM(ad_revenue.value) GROUP BY ad_type
```

#### 타이머 사용 지표:
```
✅ Timer Starts = COUNT(timer_start)
✅ Timer Completions = COUNT(timer_finish)
✅ Completion Rate = timer_finish / timer_start * 100
✅ Average Goal Days = AVG(timer_start.target_days)
```

#### 사용자 행동 지표:
```
✅ Records View Rate = COUNT(view_records) / MAU
✅ Detail View Rate = COUNT(view_record_detail) / COUNT(view_records)
```

---

### 7.2 권장 이벤트 추가 후 측정 가능한 지표

#### Retention (유지율):
```
🆕 D1 Retention = (설치 후 1일째 session_start 발생 사용자) / 신규 사용자
🆕 D7 Retention = (설치 후 7일째 session_start 발생 사용자) / 신규 사용자
🆕 D30 Retention = (설치 후 30일째 session_start 발생 사용자) / 신규 사용자
```

#### Churn (이탈):
```
🆕 Churn Rate = COUNT(timer_give_up) / COUNT(timer_start) * 100
🆕 Average Days Before Churn = AVG(timer_give_up.actual_days)
🆕 Churn by Progress = COUNT(timer_give_up) GROUP BY progress_percent (0-25%, 25-50%, 50-75%, 75-100%)
```

#### Engagement (참여도):
```
🆕 DAU (Daily Active Users) = COUNT(DISTINCT user_id WHERE session_start)
🆕 MAU (Monthly Active Users) = COUNT(DISTINCT user_id WHERE session_start, 30일)
🆕 DAU/MAU Ratio = DAU / MAU (높을수록 활성도 높음)
🆕 Session Frequency = COUNT(session_start) / DAU
🆕 Screens per Session = COUNT(screen_view) / COUNT(session_start)
```

#### LTV (Lifetime Value):
```
🆕 User Lifetime = 최초 session_start ~ 마지막 session_start (일수)
🆕 Lifetime Revenue = SUM(ad_revenue.value) per user
🆕 LTV Segments:
    - VIP Users (레벨 7 이상, 30일+ 사용)
    - Active Users (레벨 3~6, 7~30일)
    - Casual Users (레벨 1~2, ~7일)
    - Churned Users (timer_give_up 발생)
```

#### Feature Usage (기능 사용률):
```
🆕 Records Usage = COUNT(view_records) / COUNT(session_start) * 100
🆕 Community Usage = COUNT(community_post) / COUNT(session_start) * 100
🆕 Level Screen Usage = COUNT(screen_view WHERE screen_name="level_detail") / COUNT(session_start) * 100
```

#### Gamification (게임화 효과):
```
🆕 Level Distribution = COUNT(users) GROUP BY current_level
🆕 Level Up Velocity = AVG(days) per level_up
🆕 Achievement Rate = COUNT(level_up WHERE new_level >= 5) / COUNT(timer_start) * 100
```

#### Virality (바이럴):
```
🆕 Share Rate = COUNT(share_achievement) / COUNT(timer_finish) * 100
🆕 K-Factor = (Share Rate * 가입 전환율) (앱스플라이어/GA4 연동 필요)
```

---

## 8. 구현 가이드

### 8.1 `timer_give_up` 이벤트 추가 (예시)

**Step 1**: `AnalyticsEvents.kt`에 상수 추가
```kotlin
object AnalyticsEvents {
    // ...existing code...
    const val TIMER_GIVE_UP = "timer_give_up" // [NEW]
}

object AnalyticsParams {
    // ...existing code...
    const val QUIT_REASON = "quit_reason"        // [NEW]
    const val QUIT_TS = "quit_ts"                // [NEW]
    const val PROGRESS_PERCENT = "progress_percent" // [NEW]
}
```

**Step 2**: `AnalyticsManager.kt`에 함수 추가
```kotlin
object AnalyticsManager {
    // ...existing code...
    
    // [NEW] TimerGiveUp: 타이머 포기 (2025-12-31)
    fun logTimerGiveUp(
        targetDays: Int,
        actualDays: Int,
        quitReason: String,
        startTs: Long,
        quitTs: Long,
        progressPercent: Float
    ) = log(AnalyticsEvents.TIMER_GIVE_UP) {
        putInt(AnalyticsParams.TARGET_DAYS, targetDays)
        putInt(AnalyticsParams.ACTUAL_DAYS, actualDays)
        putString(AnalyticsParams.QUIT_REASON, quitReason)
        putLong(AnalyticsParams.START_TS, startTs)
        putLong(AnalyticsParams.QUIT_TS, quitTs)
        putFloat(AnalyticsParams.PROGRESS_PERCENT, progressPercent)
    }
}
```

**Step 3**: `Tab01ViewModel.kt`에서 호출
```kotlin
fun giveUpTimer() {
    viewModelScope.launch {
        try {
            // ...existing code...
            
            // [NEW] Analytics 이벤트 전송 (2025-12-31)
            try {
                val progressPercent = (actualDays / targetDays * 100).toFloat()
                AnalyticsManager.logTimerGiveUp(
                    targetDays = targetDays.toInt(),
                    actualDays = actualDays.toInt(),
                    quitReason = "user_quit",
                    startTs = startTime,
                    quitTs = endTime,
                    progressPercent = progressPercent
                )
                Log.d(TAG, "Analytics: timer_give_up event sent (progress=${progressPercent}%)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log analytics", e)
            }
            
            // ...existing code...
        } catch (e: Exception) {
            Log.e(TAG, "giveUpTimer failed", e)
        }
    }
}
```

---

### 8.2 `session_start` 이벤트 추가 (예시)

**구현 위치**: `MainActivity.onCreate()` 또는 `MainApplication.onCreate()`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ...existing code...
    
    // [NEW] Session Start 이벤트 전송 (2025-12-31)
    try {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val installTime = sharedPref.getLong("install_time", 0L)
        
        // 첫 실행이면 설치 시각 저장
        if (installTime == 0L) {
            sharedPref.edit().putLong("install_time", System.currentTimeMillis()).apply()
        }
        
        val daysSinceInstall = if (installTime > 0) {
            ((System.currentTimeMillis() - installTime) / (24 * 60 * 60 * 1000)).toInt()
        } else {
            0
        }
        
        val timerStatus = when {
            sharedPref.getBoolean("timer_completed", false) -> "completed"
            sharedPref.getLong("start_time", 0L) > 0L -> "active"
            else -> "idle"
        }
        
        AnalyticsManager.logSessionStart(
            isFirstSession = daysSinceInstall == 0,
            daysSinceInstall = daysSinceInstall,
            timerStatus = timerStatus
        )
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to log session_start", e)
    }
}
```

---

## 9. Firebase Console 대시보드 설정

### 9.1 권장 커스텀 리포트

#### 리포트 1: 타이머 퍼널 (Timer Funnel)
```
timer_start (100%)
  └─> timer_finish (목표 달성) → Completion Rate 계산
  └─> timer_give_up (포기) → Churn Rate 계산
```

#### 리포트 2: 레벨 진행 (Level Progression)
```
session_start → level_up → timer_finish
```

#### 리포트 3: 광고 수익 최적화
```
ad_impression → ad_click → ad_revenue
(by ad_type, by user_level, by screen_name)
```

#### 리포트 4: 사용자 세그먼트별 LTV
```
VIP Users (레벨 7+): Lifetime Revenue = ?
Active Users (레벨 3~6): Lifetime Revenue = ?
Casual Users (레벨 1~2): Lifetime Revenue = ?
```

---

## 10. 예상 비즈니스 임팩트

### 권장 이벤트 구현 전후 비교

| 지표 | 구현 전 | 구현 후 |
|-----|--------|--------|
| **측정 가능한 지표 수** | 8개 | 30개+ |
| **Retention 분석** | ❌ 불가능 | ✅ D1/D7/D30 측정 가능 |
| **Churn 분석** | ❌ 불가능 | ✅ 포기 시점/이유 분석 가능 |
| **LTV 계산** | ❌ 불가능 | ✅ 세그먼트별 LTV 계산 가능 |
| **기능 사용률** | 부분 측정 | ✅ 전체 화면/기능 측정 가능 |
| **UA 효율 측정** | ❌ 불가능 | ✅ 캠페인별 ROI 계산 가능 |

### 예상 수익 개선 효과

```
시나리오 1: Churn 분석으로 이탈 10% 감소
현재 MAU: 10,000명
광고 수익 per user: $0.50/월
→ 추가 수익: 1,000명 * $0.50 = $500/월

시나리오 2: LTV 기반 UA 최적화로 VIP 사용자 20% 증가
VIP 사용자 LTV: $5
신규 VIP 사용자: 200명/월
→ 추가 수익: 200 * $5 = $1,000/월

총 예상 추가 수익: $1,500/월 = $18,000/년
```

---

## 11. 체크리스트

### Phase 1 구현 (즉시) ✅

- [ ] `timer_give_up` 이벤트 추가
  - [ ] AnalyticsEvents.kt에 상수 추가
  - [ ] AnalyticsManager.kt에 함수 추가
  - [ ] Tab01ViewModel.giveUpTimer()에서 호출
  - [ ] 빌드 및 테스트

- [ ] `session_start` 이벤트 추가
  - [ ] AnalyticsEvents.kt에 상수 추가
  - [ ] AnalyticsManager.kt에 함수 추가
  - [ ] MainActivity.onCreate()에서 호출
  - [ ] 빌드 및 테스트

- [ ] `level_up` 이벤트 추가
  - [ ] AnalyticsEvents.kt에 상수 추가
  - [ ] AnalyticsManager.kt에 함수 추가
  - [ ] UserStatusManager 또는 레벨 변경 감지 로직에서 호출
  - [ ] 빌드 및 테스트

- [ ] `screen_view` 이벤트 추가
  - [ ] AnalyticsEvents.kt에 상수 추가
  - [ ] AnalyticsManager.kt에 함수 추가
  - [ ] AppNavHost 또는 각 Composable에서 호출
  - [ ] 빌드 및 테스트

### Phase 2 구현 (1개월 내)

- [ ] `community_post` 이벤트 추가
- [ ] `settings_change` 이벤트 추가
- [ ] `share_achievement` 이벤트 추가

### Firebase Console 설정

- [ ] 커스텀 리포트 생성 (Timer Funnel)
- [ ] 커스텀 리포트 생성 (Level Progression)
- [ ] 커스텀 리포트 생성 (Ad Revenue)
- [ ] 사용자 세그먼트 정의 (VIP/Active/Casual)
- [ ] Audience 설정 (재타겟팅용)

---

## 12. 참고 문서

- Firebase Analytics 베스트 프랙티스: https://firebase.google.com/docs/analytics/best-practices
- 추천 이벤트 목록: https://support.google.com/analytics/answer/9267735
- 앱 내 기존 문서: `docs/reference/ANALYTICS_EVENTS.md`
- 앱 내 기존 문서: `docs/backup-docs/ANALYTICS_EVENTS.md`

---

## 13. 작성자 노트

**작성자**: GitHub Copilot (유지보수 담당 시니어 개발자 모드)  
**작성일**: 2025-12-31  
**검토 필요 사항**:
- Firebase 프로젝트 설정 확인 (BigQuery 연동 권장)
- 개인정보 보호 정책 검토 (GDPR, 위치 정보 등)
- 이벤트 파라미터 길이 제한 (Firebase 제약 준수)

**다음 단계**:
1. Phase 1 이벤트 구현 (4.5시간 소요 예상)
2. 2주간 데이터 수집
3. Firebase Console에서 대시보드 구축
4. 데이터 기반 UX 개선 실행
5. 3개월 후 수익 임팩트 분석

---

**문서 버전**: 1.0  
**최종 수정**: 2025-12-31  
**상태**: ✅ 완료

