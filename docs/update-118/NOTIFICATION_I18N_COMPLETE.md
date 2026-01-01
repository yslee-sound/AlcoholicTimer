# ✅ 리텐션 알림 다국어(i18n) 적용 완료

**작업일**: 2026-01-02  
**목적**: 리텐션 알림 문구를 다국어로 번역하여 글로벌 사용자 지원  
**상태**: ✅ 완료

---

## 📋 작업 완료 요약

### 1단계: strings.xml 리소스 추가 ✅

4개 언어의 strings.xml 파일에 리텐션 알림 문구 추가:

| 언어 | 파일 | Key 개수 |
|------|------|---------|
| 영어 (기본) | `values/strings.xml` | 16개 |
| 한국어 | `values-ko/strings.xml` | 16개 |
| 일본어 | `values-ja/strings.xml` | 16개 |
| 인도네시아어 | `values-in/strings.xml` | 16개 |

**추가된 Key 목록**:

#### Group A (신규 유저) - 6개
- `notif_group_a_1_title` / `notif_group_a_1_body` (24시간)
- `notif_group_a_2_title` / `notif_group_a_2_body` (D+2)
- `notif_group_a_3_title` / `notif_group_a_3_body` (D+4)

#### Group B (활성 유저) - 6개
- `notif_group_b_3day_title` / `notif_group_b_3day_body`
- `notif_group_b_7day_title` / `notif_group_b_7day_body`
- `notif_group_b_30day_title` / `notif_group_b_30day_body`

#### Group C (휴식 유저) - 4개
- `notif_group_c_1_title` / `notif_group_c_1_body` (D+1)
- `notif_group_c_2_title` / `notif_group_c_2_body` (D+3)

---

### 2단계: 코드 수정 ✅

#### 수정된 파일

**1. RetentionMessages.kt**
- 하드코딩된 문자열 → Context 기반 리소스 참조로 변경
- Helper 함수 추가 (예: `getTitle1(context)`)

**Before**:
```kotlin
const val TITLE_1 = "🍺 ZERO 앱, 잊으신 건 아니죠?"
const val MESSAGE_1 = "딱 하루만 도전해보세요..."
```

**After**:
```kotlin
const val TITLE_1_RES = R.string.notif_group_a_1_title
const val MESSAGE_1_RES = R.string.notif_group_a_1_body

fun getTitle1(context: Context) = context.getString(TITLE_1_RES)
fun getMessage1(context: Context) = context.getString(MESSAGE_1_RES)
```

**2. RetentionNotificationManager.kt**
- `scheduleGroupANotifications()` - Context 기반으로 변경
- `scheduleGroupBNotifications()` - Context 기반으로 변경
- `scheduleGroupCNotifications()` - Context 기반으로 변경
- `showImmediateTestNotification()` - Group B 7day 알림 기본값으로 설정

**Before**:
```kotlin
title = RetentionMessages.GroupA.TITLE_1,
message = RetentionMessages.GroupA.MESSAGE_1,
```

**After**:
```kotlin
title = RetentionMessages.GroupA.getTitle1(context),
message = RetentionMessages.GroupA.getMessage1(context),
```

**3. MainActivity.kt**
- 테스트 버튼을 Group B 7day 알림(배지 획득)으로 변경
- 이모지와 긴 텍스트 UI 확인용

**Before**:
```kotlin
showImmediateTestNotification(
    context = activity,
    title = "🔔 테스트 알림입니다",
    message = "아이콘과 배너가 잘 보이나요?"
)
```

**After**:
```kotlin
showImmediateTestNotification(
    context = activity
    // Group B 7day 기본값 사용
)
```

---

## 🌏 다국어 문구 예시

### Group B 7day (배지 획득) - 테스트 버튼에서 사용

| 언어 | 제목 | 내용 |
|------|------|------|
| 🇰🇷 한국어 | Lv.1 새싹 배지 획득 🏆 | 축하합니다! 'Lv.1 새싹 배지'를 획득했습니다! 지금 수령하세요. |
| 🇺🇸 영어 | Badge Acquired! 🏆 | Congrats! You got the 'Lv.1 Sprout Badge'! Claim it now. |
| 🇯🇵 일본어 | バッジ獲得！🏆 | おめでとうございます！「Lv.1 新芽バッジ」を獲得しました！今すぐ受け取りましょう。 |
| 🇮🇩 인도네시아어 | Lencana Didapat! 🏆 | Selamat! Kamu dapat 'Lencana Tunas Lv.1'! Ambil sekarang. |

### Group A (신규 유저)

#### 24시간 후 (1차)

| 언어 | 제목 | 내용 |
|------|------|------|
| 🇰🇷 한국어 | 잊으신 건 아니죠? 👀 | 어제 설치한 ZERO 앱, 잊으신 건 아니죠? 딱 하루만 도전해봐요! |
| 🇺🇸 영어 | Did you forget? 👀 | Did you forget the ZERO app? Challenge yourself for just one day! |
| 🇯🇵 일본어 | 忘れていませんか？👀 | 昨日入れたZEROアプリ、忘れていませんか？1日だけ挑戦してみましょう！ |
| 🇮🇩 인도네시아어 | Lupa ya? 👀 | Lupa sama aplikasi ZERO? Coba tantang dirimu satu hari saja! |

---

## 🧪 테스트 방법

### 방법 1: 테스트 버튼 사용 (UI 검증)

1. 앱 실행 및 초기화 완료
2. 화면 하단의 **"🔔 알림 즉시 테스트"** 버튼 클릭
3. Group B 7day 알림 (배지 획득) 표시 확인

**확인 포인트**:
- [ ] 이모지(🏆)가 제목에 표시됨
- [ ] BigTextStyle로 긴 텍스트가 전부 보임
- [ ] 언어별로 올바른 번역이 표시됨

### 방법 2: 에뮬레이터 언어 변경

#### 한국어 테스트
```powershell
adb -s emulator-5554 shell "setprop persist.sys.locale ko-KR; setprop ctl.restart zygote"
```

#### 영어 테스트
```powershell
adb -s emulator-5554 shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"
```

#### 일본어 테스트
```powershell
adb -s emulator-5554 shell "setprop persist.sys.locale ja-JP; setprop ctl.restart zygote"
```

#### 인도네시아어 테스트
```powershell
adb -s emulator-5554 shell "setprop persist.sys.locale in-ID; setprop ctl.restart zygote"
```

### 방법 3: 실제 예약 알림 테스트 (TEST_MODE)

```kotlin
// RetentionNotificationManager.kt
private const val TEST_MODE = true  // ⚠️ 이미 활성화됨

// 테스트 시간
- 1차: 10초 후
- 2차: 20초 후
- 3차: 30초 후
```

1. 앱 재설치 및 실행
2. 권한 허용 완료
3. 10초 대기 → 1차 알림 확인
4. 20초 대기 → 2차 알림 확인
5. 30초 대기 → 3차 알림 확인

---

## 📊 언어별 문구 전체 목록

### 🇰🇷 한국어

| 그룹 | 시점 | 제목 | 내용 |
|------|------|------|------|
| A | 24H | 잊으신 건 아니죠? 👀 | 어제 설치한 ZERO 앱, 잊으신 건 아니죠? 딱 하루만 도전해봐요! |
| A | D+2 | 오늘이 바로 그날! 🔥 | 작심삼일도 시작을 해야 할 수 있어요! 오늘이 그날입니다. |
| A | D+4 | 벌써 일주일... 🍺 | 벌써 일주일이 지났어요. 술값 아껴서 사고 싶은 게 있지 않으신가요? |
| B | 3일 | 작심삼일 탈출! 🎉 | 작심삼일 탈출! 첫 번째 고비를 넘겼어요. 탭해서 확인하세요. |
| B | 7일 | Lv.1 새싹 배지 획득 🏆 | 축하합니다! 'Lv.1 새싹 배지'를 획득했습니다! 지금 수령하세요. |
| B | 30일 | 치킨 5마리 값 벌었어요 💸 | 와! 벌써 15만 원이나 아꼈어요. 치킨 5마리 값입니다! |
| C | D+1 | 3일 성공 대단했어요! 👍 | 3일 성공 대단했어요! 이제 '일주일' 코스에 도전해볼까요? |
| C | D+3 | 간이 휴식을 원해요 🏥 | 다시 달릴 준비 되셨나요? 당신의 간이 휴식을 기다리고 있어요. |

### 🇺🇸 영어

| 그룹 | 시점 | 제목 | 내용 |
|------|------|------|------|
| A | 24H | Did you forget? 👀 | Did you forget the ZERO app? Challenge yourself for just one day! |
| A | D+2 | Today is the day! 🔥 | You have to start to quit! Today is the day to begin. |
| A | D+4 | Already a week... 🍺 | It's been a week. Want to buy something with the money saved on drinks? |
| B | 3일 | 3-Day Bump Passed! 🎉 | You passed the 3-day mark! You overcame the first hurdle. Tap to check. |
| B | 7일 | Badge Acquired! 🏆 | Congrats! You got the 'Lv.1 Sprout Badge'! Claim it now. |
| B | 30일 | You saved big money 💸 | Wow! You saved about $150. That's a huge saving! |
| C | D+1 | Great job on 3 days! 👍 | Your 3-day streak was great! Shall we try the '1 Week Course' now? |
| C | D+3 | Your liver needs rest 🏥 | Ready to start again? Your liver is waiting for a break. |

### 🇯🇵 일본어

| 그룹 | 시점 | 제목 | 내용 |
|------|------|------|------|
| A | 24H | 忘れていませんか？👀 | 昨日入れたZEROアプリ、忘れていませんか？1日だけ挑戦してみましょう！ |
| A | D+2 | 今日がその日です！🔥 | 三日坊主も始めなければ克服できません！今日こそ始めましょう。 |
| A | D+4 | もう1週間… 🍺 | もう1週間経ちました。お酒代を節約して、欲しいものを買いませんか？ |
| B | 3일 | 三日坊主を脱出！🎉 | 三日坊主を卒業！最初の山場を越えました。タップして確認。 |
| B | 7일 | バッジ獲得！🏆 | おめでとうございます！「Lv.1 新芽バッジ」を獲得しました！今すぐ受け取りましょう。 |
| B | 30일 | かなりの節約です 💸 | わあ！もう1万5千円も節約しました。美味しいものが食べられますよ！ |
| C | D+1 | 3日間、すごいです！👍 | 3日間の成功は立派でした！次は「1週間コース」に挑戦しませんか？ |
| C | D+3 | 肝臓が休みたいそうです 🏥 | 準備はいいですか？あなたの肝臓が休息を待っています。 |

### 🇮🇩 인도네시아어

| 그룹 | 시점 | 제목 | 내용 |
|------|------|------|------|
| A | 24H | Lupa ya? 👀 | Lupa sama aplikasi ZERO? Coba tantang dirimu satu hari saja! |
| A | D+2 | Hari ini saatnya! 🔥 | Kamu harus mulai untuk bisa berhenti! Hari ini saatnya. |
| A | D+4 | Sudah seminggu... 🍺 | Sudah seminggu berlalu. Mau beli sesuatu dari uang jajan yang dihemat? |
| B | 3일 | Lolos 3 Hari Pertama! 🎉 | Kamu melewati 3 hari pertama! Rintangan awal sudah lewat. Ketuk untuk cek. |
| B | 7일 | Lencana Didapat! 🏆 | Selamat! Kamu dapat 'Lencana Tunas Lv.1'! Ambil sekarang. |
| B | 30일 | Hemat banyak uang 💸 | Wah! Kamu sudah hemat Rp1,5 Juta. Bisa buat makan besar! |
| C | D+1 | Kerja bagus 3 harinya! 👍 | 3 harimu luar biasa! Mau coba 'Target 1 Minggu' sekarang? |
| C | D+3 | Liver-mu butuh istirahat 🏥 | Siap mulai lagi? Liver-mu menanti istirahat darimu. |

---

## ✅ 최종 체크리스트

### 리소스 추가
- [x] values/strings.xml - 16개 Key 추가
- [x] values-ko/strings.xml - 16개 Key 추가
- [x] values-ja/strings.xml - 16개 Key 추가
- [x] values-in/strings.xml - 16개 Key 추가

### 코드 수정
- [x] RetentionMessages.kt - Context 기반으로 변경
- [x] RetentionNotificationManager.kt - Group A, B, C 모두 수정
- [x] MainActivity.kt - 테스트 버튼 Group B 7day로 변경
- [x] showImmediateTestNotification - 기본값 설정

### 테스트 준비
- [x] 테스트 버튼 작동 확인
- [x] 에뮬레이터 언어 변경 명령어 준비
- [x] 컴파일 에러 0개

---

## 🎯 기대 효과

### 사용자 경험 개선
- ✅ 사용자의 언어로 알림 메시지 표시
- ✅ 이모지와 긴 텍스트가 자연스럽게 표시
- ✅ 현지화된 표현으로 공감대 형성

### 글로벌 확장
- 🌏 한국, 미국, 일본, 인도네시아 시장 대응 완료
- 🌍 필리핀 등 추가 시장 진출 준비 완료
- 🌎 다국어 지원으로 글로벌 앱 경쟁력 강화

---

## 📝 다음 단계 (선택사항)

### 우선순위 낮음

**1. 필리핀어 번역 추가**
```
현재: 영어 대체 사용
향후: values-tl/strings.xml 추가
```

**2. 테스트 버튼 제거**
```
배포 전: 테스트 버튼 주석 처리 또는 BuildConfig.DEBUG 조건 추가
```

**3. TEST_MODE 비활성화**
```
배포 전: TEST_MODE = false로 변경
```

---

**작성일**: 2026-01-02  
**상태**: ✅ 완료  
**다음 단계**: 에뮬레이터에서 언어별 테스트 진행

