# 개인정보 설정(Privacy Options) 위치 가이드

**작성일:** 2025-12-12  
**목적:** Privacy Options 기능의 올바른 위치를 명확히 정의하여 중복 구현 방지

---

## ⚠️ 중요 원칙

### ✅ Privacy Options 기능은 **Tab05(더보기/About)에만** 존재해야 합니다.

### ❌ Tab04(설정)에는 절대 추가하지 마세요!

---

## 📂 앱 화면 구조

```
AlcoholicTimer 앱
├─ Tab 1: 홈 (Start/Run/Quit)
├─ Tab 2: 기록 (Records)
├─ Tab 3: 레벨 (Level)
├─ Tab 4: 설정 (Settings) ← ❌ Privacy Options 넣지 말 것!
│   ├─ 음주 비용 설정
│   ├─ 음주 빈도 설정
│   ├─ 음주 시간 설정
│   └─ 통화 설정
│
└─ Tab 5: 더보기 (About/More) ← ✅ Privacy Options는 여기만!
    ├─ Privacy Policy
    ├─ Open Source License
    ├─ Privacy Options ← 여기!
    ├─ Customer Feedback
    ├─ App Version
    └─ Debug (개발 모드)
```

---

## 🎯 각 탭의 명확한 역할

### Tab 4: 설정 (Settings)
**파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_04/Tab04.kt`

**역할:** 앱의 **기능적 설정**만 다룸
- 음주 비용 설정 (저/중/고)
- 음주 빈도 설정 (저/중/고)
- 음주 시간 설정 (짧음/중간/긺)
- 통화 설정 (자동/수동 선택)

**포함하지 않아야 할 것:**
- ❌ 개인정보 설정
- ❌ Privacy Options
- ❌ 광고 설정
- ❌ 정책 관련 메뉴

---

### Tab 5: 더보기/About (More)
**파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_05/Tab05.kt`

**역할:** 앱의 **정보 및 정책** 관련 메뉴
- Privacy Policy (개인정보 처리방침)
- Open Source License (오픈소스 라이선스)
- **Privacy Options (개인정보 설정)** ← 여기!
- Customer Feedback (고객 문의)
- App Version (버전 정보)
- Debug Menu (개발 모드 전용)

**Privacy Options 구현:**
```kotlin
// Tab05.kt - AboutScreen 내부
onAdsClick = {
    val activity = ContextToActivity(context)
    if (activity != null && umpConsentManager != null) {
        try {
            umpConsentManager.showPrivacyOptionsForm(activity) { error ->
                if (error != null) {
                    Log.e("AboutScreen", "Privacy Options Form 표시 실패: $error")
                    Toast.makeText(context, "개인정보 설정을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("AboutScreen", "Privacy Options Form 정상 표시 완료")
                }
            }
        } catch (t: Throwable) {
            Log.e("AboutScreen", "showPrivacyOptionsForm 호출 실패", t)
        }
    }
}
```

---

## 🚫 중복 구현 방지 체크리스트

새로운 기능을 추가하거나 수정할 때 다음을 확인하세요:

### ✅ Privacy Options 관련 작업 시:
- [ ] Tab05(About)에만 코드를 수정하고 있는가?
- [ ] Tab04(Settings)를 건드리지 않았는가?
- [ ] `umpConsentManager.showPrivacyOptionsForm()`은 Tab05에만 호출되는가?

### ✅ 설정 관련 작업 시:
- [ ] Tab04(Settings)에 추가하는 것이 **기능적 설정**(비용, 빈도 등)인가?
- [ ] 정책/개인정보 관련 내용이 아닌가?

---

## 📝 Google UMP 정책 준수

### Privacy Options를 Tab05에만 두는 이유:

1. **UI/UX 일관성**
   - Privacy Policy, License 등 **정책 관련 메뉴**와 함께 위치
   - 사용자가 **"앱 정보 및 정책"을 한 곳에서 확인** 가능

2. **Google 권장 사항**
   - "Privacy Options"는 **About 화면이나 Settings의 Privacy 섹션**에 위치해야 함
   - 우리 앱은 About 화면(Tab05)을 선택

3. **기능 분리 원칙**
   - Tab04: 앱 동작 설정 (비용, 빈도 등)
   - Tab05: 앱 정보 및 정책 (Privacy, License 등)

---

## 🔧 과거 실수 기록

### 2025-12-12: Tab04에 중복 추가 (수정 완료)
- **문제:** Tab04(설정)에 "개인정보 및 광고" 섹션을 실수로 추가함
- **원인:** Tab04와 Tab05의 역할 구분이 명확하지 않았음
- **조치:** 
  - Tab04에서 개인정보 관련 코드 완전 제거
  - 본 가이드 문서 작성
  - 불필요한 import 정리

**제거된 코드 (Tab04.kt):**
```kotlin
// ❌ 절대 다시 추가하지 말 것!
SettingsSection(
    title = "개인정보 및 광고",  // Tab05에만 있어야 함!
    ...
) {
    Row(...) {
        ...
        umpConsentManager.showPrivacyOptionsForm(activity) { ... }
    }
}
```

---

## 🎉 올바른 구현 (Tab05만)

```
앱 실행
  ↓
Tab 5 (더보기) 클릭
  ↓
"Privacy Options" 메뉴 클릭
  ↓
UMP Privacy Options Form 표시
  ↓
사용자가 동의/거부 선택
  ↓
About 화면으로 복귀
```

---

## 📚 참고 문서

- **Google UMP 가이드:** [User Messaging Platform](https://developers.google.com/admob/android/privacy/gdpr)
- **관련 파일:**
  - Tab05: `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_05/Tab05.kt`
  - UMP Manager: `app/src/main/java/kr/sweetapps/alcoholictimer/consent/UmpConsentManager.kt`

---

## ⚡ 빠른 참조

| 기능 | 위치 | 파일 |
|------|------|------|
| 음주 비용/빈도/시간 설정 | Tab04 (설정) | `ui/tab_04/Tab04.kt` |
| 통화 설정 | Tab04 (설정) | `ui/tab_04/Tab04.kt` |
| Privacy Options | Tab05 (더보기) | `ui/tab_05/Tab05.kt` |
| Privacy Policy | Tab05 (더보기) | `ui/tab_05/Tab05.kt` |
| Open Source License | Tab05 (더보기) | `ui/tab_05/Tab05.kt` |

---

**작성자:** GitHub Copilot  
**최종 수정일:** 2025-12-12  
**버전:** 1.0

