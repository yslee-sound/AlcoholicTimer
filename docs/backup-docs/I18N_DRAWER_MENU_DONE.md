# 드로어 메뉴 다국어화 완료

> 📋 드로어 메뉴와 프로필 별명이 다국어로 적용되었습니다.

최종 수정: 2025-10-27

---

## ✅ 완료된 작업

### 1. strings.xml 추가 (한국어/영어)

#### 한국어 (`values/strings.xml`)
```xml
<!-- Drawer menu (네비게이션 드로어) -->
<string name="default_nickname">알중이1</string>
<string name="drawer_edit_profile">프로필 편집</string>
<string name="drawer_section_menu">메뉴</string>
<string name="drawer_section_settings">설정</string>
<string name="drawer_menu_sobriety">금주</string>
<string name="drawer_menu_records">기록</string>
<string name="drawer_menu_level">레벨</string>
<string name="drawer_menu_settings">설정</string>
<string name="drawer_menu_about">앱 정보</string>
<string name="cd_avatar">아바타</string>
<string name="cd_menu">메뉴</string>
```

#### 영어 (`values-en/strings.xml`)
```xml
<!-- Drawer menu (Navigation drawer) -->
<string name="default_nickname">SoberHero1</string>
<string name="drawer_edit_profile">Edit Profile</string>
<string name="drawer_section_menu">Menu</string>
<string name="drawer_section_settings">Settings</string>
<string name="drawer_menu_sobriety">Sobriety</string>
<string name="drawer_menu_records">Records</string>
<string name="drawer_menu_level">Level</string>
<string name="drawer_menu_settings">Settings</string>
<string name="drawer_menu_about">About</string>
<string name="cd_avatar">Avatar</string>
<string name="cd_menu">Menu</string>
```

### 2. 코드 수정

#### BaseActivity.kt
- ✅ `com.sweetapps.alcoholictimer.R` import 추가
- ✅ 기본 닉네임: `"알중이1"` → `getString(R.string.default_nickname)`
- ✅ 드로어 메뉴 항목: 하드코딩 → `context.getString(R.string.*)`
- ✅ 섹션 타이틀: `"메뉴"`, `"설정"` → 리소스
- ✅ TopAppBar contentDescription: `"메뉴"`, `"뒤로가기"` → 리소스
- ✅ `handleMenuSelection()`: 문자열 비교를 리소스 기반으로 변경
- ✅ `currentDrawerSelection()`: 리소스 기반으로 변경

#### NicknameEditActivity.kt
- ✅ `com.sweetapps.alcoholictimer.R` import 추가
- ✅ 기본 닉네임: `"알중이1"` → `getString(R.string.default_nickname)`

---

## 🌍 언어별 표시 내용

### 한국어 모드
- 프로필 별명 기본값: **알중이1**
- 프로필 편집: **프로필 편집**
- 섹션: **메뉴**, **설정**
- 메뉴 항목:
  - 금주
  - 기록
  - 레벨
  - 설정
  - 앱 정보

### 영어 모드 (English)
- Default nickname: **SoberHero1**
- Profile edit: **Edit Profile**
- Sections: **Menu**, **Settings**
- Menu items:
  - Sobriety
  - Records
  - Level
  - Settings
  - About

---

## 🧪 테스트 방법

### 1. 빌드
```cmd
cd G:\Workspace\AlcoholicTimer
.\gradlew.bat assembleDebug
```

### 2. 언어 변경
```cmd
# 영어로 변경
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"

# 한국어로 변경
adb shell "setprop persist.sys.locale ko-KR; setprop ctl.restart zygote"
```

### 3. 확인 사항
- [ ] 드로어 메뉴 열기
- [ ] 프로필 별명이 언어에 맞게 표시되는가?
  - 한국어: "알중이1"
  - 영어: "SoberHero1"
- [ ] 프로필 편집 텍스트 확인
  - 한국어: "프로필 편집"
  - 영어: "Edit Profile"
- [ ] 메뉴 섹션 타이틀 확인
  - 한국어: "메뉴", "설정"
  - 영어: "Menu", "Settings"
- [ ] 메뉴 항목 확인
  - 한국어: 금주, 기록, 레벨, 설정, 앱 정보
  - 영어: Sobriety, Records, Level, Settings, About
- [ ] 각 메뉴 항목 클릭 시 정상 동작

---

## 📝 변경 내역

### 추가된 리소스
- `default_nickname`: 기본 프로필 별명
- `drawer_edit_profile`: 프로필 편집 버튼 텍스트
- `drawer_section_menu`: "메뉴" 섹션 타이틀
- `drawer_section_settings`: "설정" 섹션 타이틀
- `drawer_menu_sobriety`: 금주 메뉴
- `drawer_menu_records`: 기록 메뉴
- `drawer_menu_level`: 레벨 메뉴
- `drawer_menu_settings`: 설정 메뉴
- `drawer_menu_about`: 앱 정보 메뉴
- `cd_avatar`: 아바타 아이콘 설명
- `cd_menu`: 메뉴 아이콘 설명

### 수정된 파일
1. `app/src/main/res/values/strings.xml`
2. `app/src/main/res/values-en/strings.xml`
3. `app/src/main/java/.../core/ui/BaseActivity.kt`
4. `app/src/main/java/.../feature/profile/NicknameEditActivity.kt`

---

## 🎯 다음 단계

### 기능 확장 가능성
1. **언어별 기본 닉네임 커스터마이징**
   - 일본어: "禁酒マスター1"
   - 중국어: "戒酒英雄1"
   
2. **사용자 지정 닉네임 유지**
   - 사용자가 한 번 닉네임을 변경하면 언어 변경 시에도 유지

3. **드로어 메뉴 아이콘 로컬라이제이션**
   - 필요 시 언어별 다른 아이콘 사용 가능

---

## 💡 참고사항

### 기본 닉네임 설정 로직
사용자가 닉네임을 설정하지 않은 경우:
- 한국어: "알중이1"
- 영어: "SoberHero1"

사용자가 닉네임을 설정한 경우:
- 언어와 상관없이 사용자가 설정한 닉네임 표시

### 드로어 메뉴 선택 상태
현재 화면에 따라 드로어 메뉴 항목이 자동으로 선택됩니다:
- RunActivity/StartActivity → "금주" / "Sobriety"
- RecordsActivity → "기록" / "Records"
- LevelActivity → "레벨" / "Level"
- SettingsActivity → "설정" / "Settings"
- AboutActivity → "앱 정보" / "About"

---

**작성일**: 2025-10-27  
**버전**: 1.0  
**상태**: ✅ 빌드 진행 중

**빌드 완료 후**: 에뮬레이터에서 언어 전환하여 확인하세요!

