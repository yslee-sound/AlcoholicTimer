# 📋 Firebase Analytics 이벤트 빠른 참조 (Quick Reference)

**버전**: v1.1.9  
**날짜**: 2026-01-02

---

## 🎯 10개 핵심 이벤트 한눈에 보기

| # | 이벤트 | 그룹 | 호출 위치 | 상태 |
|---|--------|------|-----------|------|
| 1 | `ad_revenue` | 💰 | AdBanner, InterstitialAdManager | ✅ |
| 2 | `ad_impression` | 💰 | AdBanner, InterstitialAdManager, AppOpenAdManager | ✅ |
| 3 | `ad_click` | 💰 | AdBanner, InterstitialAdManager, AppOpenAdManager | ✅ |
| 4 | `timer_start` | 🔥 | StartScreenViewModel.startCountdown() | ✅ |
| 5 | `timer_give_up` | 🔥 | Tab01ViewModel.giveUpTimer() | ✅ |
| 6 | `diary_save` | 🔥 | DiaryWriteScreen (일기 저장 시) | ✅ |
| 7 | `community_post` | 🔥 | CommunityViewModel.savePost() | ✅ |
| 8 | `level_up` | 🌱 | UserStatusManager.updateUserLevel() | ✅ |
| 9 | `session_start` | 🏥 | MainActivity.onCreate() | ✅ |
| 10 | `notification_open` | 🏥 | MainActivity.handleDeepLink() | ✅ |

**진행률**: 10/10 완료 (100%) 🎉

---

## 🗑️ 삭제된 이벤트 (사용 금지)

| 이벤트 | 이유 |
|--------|------|
| `view_records` | 노이즈 이벤트 |
| `change_record_view` | 노이즈 이벤트 |
| `view_record_detail` | 노이즈 이벤트 |
| `screen_view` | 데이터 폭증 |

⚠️ **위 이벤트는 절대 사용하지 말 것!**

---

## 📊 핵심 지표 계산식

```
ARPU = SUM(ad_revenue) / DAU
CTR = ad_click / ad_impression * 100
D-7 Retention = session_start(day=7) / 신규 설치
Churn Rate = timer_give_up / timer_start
```

---

## 🎉 작업 완료!

**모든 핵심 이벤트가 구현되었습니다!**

- ✅ 10개 이벤트 100% 구현 완료
- ✅ 불필요한 이벤트 제거 완료
- ✅ 문서 최신화 완료

---

**빠른 검색**:
- 전체 문서: `FIREBASE_ANALYTICS_FINAL_12_EVENTS.md`
- 구현 위치: `AnalyticsManager.kt`
- 상수 정의: `AnalyticsEvents.kt`

